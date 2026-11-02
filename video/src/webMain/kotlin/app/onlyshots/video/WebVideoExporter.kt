package app.onlyshots.video

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.onlyshots.core.model.Quality
import app.onlyshots.core.model.Reframe
import app.onlyshots.core.model.VideoItem
import app.onlyshots.core.model.overlayTitle
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.core_export_no_webcodecs
import app.onlyshots.core.resources.core_export_no_webgl
import app.onlyshots.overlay.GraphStyles
import app.onlyshots.overlay.compose.OVERLAY_TEXT_CACHE
import app.onlyshots.overlay.compose.OverlayFonts
import app.onlyshots.overlay.compose.OverlayFrameRenderer
import app.onlyshots.overlay.compose.overlayLabels
import kotlin.math.roundToInt
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import okio.Path
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/**
 * The web editor's export: Mediabunny decodes the trimmed clip and draws each frame cover-filled and reframed at the
 * output size, [SkiaFrameDrawer] adds the blur, glass and graph exactly as the iOS and desktop exporters do, and
 * WebCodecs encodes the MP4 through Mediabunny. Always at 1080 on the short side; the result is registered at [out] for
 * the download.
 */
class WebVideoExporter(
    private val videoPath: (VideoItem) -> Path,
    private val fonts: suspend () -> OverlayFonts,
) : VideoExporter {

    override fun export(
        item: VideoItem,
        quality: Quality,
        withSound: Boolean,
        out: Path,
        alignmentBarcode: Boolean,
    ): Flow<ExportProgress> = channelFlow {
        val overlayFonts = fonts()
        send(ExportProgress.Rendering(0f))
        val result = runCatching {
            render(item, Quality.HD_1080, withSound, out, alignmentBarcode, overlayFonts) { f ->
                trySend(ExportProgress.Rendering(f))
            }
        }
        val error = result.exceptionOrNull()
        if (error is kotlinx.coroutines.CancellationException) throw error
        send(
            when (error) {
                null -> ExportProgress.Done(out)
                is NoWebGlException -> ExportProgress.Failed(getString(Res.string.core_export_no_webgl))
                is NoWebCodecsException -> ExportProgress.Failed(getString(Res.string.core_export_no_webcodecs))
                else -> ExportProgress.Failed(error.message ?: error::class.simpleName.orEmpty())
            }
        )
    }

    private suspend fun render(
        item: VideoItem,
        quality: Quality,
        withSound: Boolean,
        out: Path,
        alignmentBarcode: Boolean,
        overlayFonts: OverlayFonts,
        progress: (Float) -> Unit,
    ) {
        val labels = overlayLabels()
        val source = videoPath(item).toString()
        val size = quality.size(item.format)
        val w = size.width
        val h = size.height
        val r = item.reframe
        // The reframe as media.js draws it: zoom on the cover fit, the centre moved by a fraction of the drawn size.
        // The
        // drawn picture is wider (or taller) than the frame where the crop keeps only kx (ky) of it.
        val sourceAspect = runCatching { WebVideoProbe().info(videoPath(item)).aspect }.getOrNull()
        val kx = sourceAspect?.let { minOf(1f, item.format.ratio / it) } ?: 1f
        val ky = sourceAspect?.let { minOf(1f, it / item.format.ratio) } ?: 1f
        val fps =
            WebMedia.exportStart(
                source,
                w,
                h,
                item.trimInUs / 1_000_000.0,
                item.trimOutUs / 1_000_000.0,
                r.zoom.toDouble(),
                ((0.5f - r.x) * kx).toDouble(),
                ((0.5f - r.y) * ky).toDouble(),
                withSound && item.hasAudio,
            )
        var finished = false
        try {
            val shot = item.shot
            val shotStart = item.shotStartInVideoUs
            val style = shot?.let { GraphStyles.resolve(item.overlay, it.source) }
            val luma =
                if (style?.adapt?.enabled == true) {
                    LumaTrack.measure(
                        WebFrameSampler(),
                        videoPath(item),
                        item.overlay.rect,
                        item.trimInUs,
                        item.trimOutUs,
                        item.sourceMapping(sourceAspect),
                    )
                } else null
            val renderer =
                if (shot != null && shotStart != null && style != null) {
                    OverlayFrameRenderer(
                        shot = shot,
                        shotStartUs = shotStart,
                        settings = item.overlay,
                        style = style,
                        title = overlayTitle(shot, item.overlay, labels.scale),
                        labels = labels,
                        recipe = item.overlay.recipe,
                        frameWidth = w,
                        frameHeight = h,
                        textMeasurer =
                            TextMeasurer(
                                createFontFamilyResolver(),
                                Density(1f),
                                LayoutDirection.Ltr,
                                OVERLAY_TEXT_CACHE,
                            ),
                        fonts = overlayFonts,
                        alignmentBarcode = alignmentBarcode,
                        fps = fps.roundToInt(),
                        backdropLuma = { t -> luma?.at(t) },
                    )
                } else null
            val overlayBitmap = renderer?.newBitmap()
            val info = ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
            // The browser writes each decoded frame into [input] and reads the finished one out of [output]'s pixels,
            // Skia's memory both ways; in between, the frame is drawn on the GPU.
            val input = Bitmap().apply { allocPixels(info) }
            val output = Bitmap().apply { allocPixels(info) }
            val gpu = WebGpu(w, h)
            val surface = gpu.use { gpu.surface(w, h) }
            // media.js has already reframed the picture; this draws it 1:1.
            val drawer = gpu.use {
                SkiaFrameDrawer(w, h, rotation = 0, reframe = Reframe(), makeSurface = gpu::surface)
            }
            val totalUs = (item.trimOutUs - item.trimInUs).coerceAtLeast(1)
            val frameSeconds = 1.0 / fps
            try {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val seconds = WebMedia.exportNext()
                    if (seconds < 0) break
                    val videoUs = (seconds * 1_000_000).toLong()
                    if (videoUs >= item.trimOutUs) break
                    WebMedia.exportFrameInto(input)
                    input.notifyPixelsChanged()
                    if (renderer != null && overlayBitmap != null) renderer.renderInto(overlayBitmap, videoUs)
                    gpu.use {
                        val picture = Image.makeFromBitmap(input)
                        drawer.draw(
                            picture,
                            surface,
                            overlayBitmap?.let { Triple(it, renderer!!.left, renderer.top) },
                            renderer?.backdrops(videoUs).orEmpty(),
                        )
                        picture.close()
                        surface.readPixels(output, 0, 0)
                    }
                    val at = ((videoUs - item.trimInUs) / 1_000_000.0).coerceAtLeast(0.0)
                    WebMedia.exportAddFrom(output, at, frameSeconds)
                    progress(((videoUs - item.trimInUs).toFloat() / totalUs).coerceIn(0f, 1f))
                    // The awaits above resume as microtasks, which never let the page repaint: a timer does, so the
                    // progress moves and the tab stays responsive while the export runs on the one thread there is.
                    delay(1)
                }
                WebMedia.exportFinish(out.toString())
                finished = true
            } finally {
                gpu.use { surface.close() }
                gpu.close()
                input.close()
                output.close()
            }
        } finally {
            if (!finished) withContext(NonCancellable) { WebMedia.exportCancel() }
        }
    }
}
