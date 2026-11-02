package app.onlyshots.video

import app.onlyshots.core.model.NormRect
import app.onlyshots.core.model.Origin
import app.onlyshots.core.model.OverlaySettings
import app.onlyshots.core.model.Quality
import app.onlyshots.core.model.ShotFiles
import app.onlyshots.core.model.VideoFormat
import app.onlyshots.core.model.VideoItem
import app.onlyshots.core.model.style.GraphStyle
import app.onlyshots.core.model.style.TargetLook
import app.onlyshots.overlay.GraphStyles
import app.onlyshots.overlay.compose.OverlayFonts
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath

/**
 * Renders the 16:9 master for the App Store product page header (`design/header/`), through the app's own exporter so
 * the art is literally what the app makes. Not a test of anything: it is skipped unless it is handed a video, which is
 * why `./gradlew :video:desktopTest` stays green without one.
 *
 *     ./gradlew :video:desktopTest --tests '*HeaderMasterTest' \
 *       -Donlyshots.header.video=/path/IMG_7462.MOV \
 *       -Donlyshots.header.shot=/path/shot.json \
 *       -Donlyshots.header.out=/path/master.mp4 \
 *       -Donlyshots.header.shotStartUs=1000000 \
 *       -Donlyshots.header.durationUs=19498333
 *
 * The shot file is anything [ShotFiles.parse] reads — here a visualizer.coffee response. `shotStartUs` is where the
 * shot begins in the video, which the editor would work out from the two clocks; see design/header/README.md.
 */
class HeaderMasterTest {
    /**
     * Liquid Glass pushed towards what the system material does on a phone, for a 3840-wide frame where the built-in's
     * restraint reads as timidity: the lens bent as far as it goes with a wider colour split and a brighter rim, no
     * border around the readings, their values at regular weight, thinner curves and targets that stay out of the way.
     *
     * A copy kept with the video ([OverlaySettings.style]), not an edit to the built-in: it is one marketing frame's
     * taste, and the built-in is what users and the screenshot goldens see.
     */
    private val HEADER_STYLE: GraphStyle =
        GraphStyles.LiquidGlass.let { base ->
            base.copy(
                id = "header-glass",
                name = "Header Glass",
                builtIn = false,
                panel =
                    base.panel.copy(
                        glass =
                            base.panel.glass.copy(
                                frost = 1f,
                                refraction = 0.5f,
                                curve = 0.45f,
                                edge = 0.08f,
                                saturation = 1.3f,
                                dispersion = 0.5f,
                            )
                    ),
                lines = base.lines.copy(main = 2.2f, targets = TargetLook.FAINT, targetOpacity = 0.3f),
                cells = base.cells.copy(border = 0, valueWeight = 400),
            )
        }

    private fun prop(name: String): String? = System.getProperty("onlyshots.header.$name")?.takeIf { it.isNotBlank() }

    /** `x,y,w,h` as fractions of the frame. */
    private fun rect(): NormRect {
        val v = prop("rect")?.split(",")?.map { it.trim().toFloat() } ?: return NormRect(0.52f, 0.44f, 0.42f, 0.34f)
        require(v.size == 4) { "onlyshots.header.rect wants x,y,w,h" }
        return NormRect(v[0], v[1], v[2], v[3])
    }

    @Test
    fun rendersTheHeaderMaster() {
        val videoPath = prop("video") ?: return println("HeaderMasterTest: no onlyshots.header.video, skipping")
        val shotPath = checkNotNull(prop("shot")) { "onlyshots.header.shot is required with onlyshots.header.video" }
        val out = File(prop("out") ?: "${System.getProperty("java.io.tmpdir")}/header-master.mp4")
        val durationUs = checkNotNull(prop("durationUs")?.toLong()) { "onlyshots.header.durationUs is required" }
        val shotStartUs = prop("shotStartUs")?.toLong() ?: 0L

        val shot = ShotFiles.parse(File(shotPath).readText(), fallbackId = "header") { Instant.fromEpochSeconds(0) }
        val item =
            VideoItem(
                id = "header",
                file = File(videoPath).absolutePath,
                createdAt = Instant.fromEpochSeconds(0),
                durationUs = durationUs,
                origin = Origin.IMPORTED,
                hasAudio = true,
                shot = shot,
                shotStartInVideoUs = shotStartUs,
                syncLocked = false,
                trimInUs = 0L,
                trimOutUs = durationUs,
                // The header keeps a 1646 px band of a 4K 16:9 frame, so the default bottom-left placement would
                // lose its lower edge. The default here sits in the right half, clear of the furniture's copy, and
                // inside the band; `rect` moves it when the band does.
                overlay =
                    OverlaySettings(
                        styleId = HEADER_STYLE.id,
                        style = HEADER_STYLE,
                        rect = rect(),
                        title = prop("title"),
                    ),
                format = VideoFormat.LANDSCAPE_16_9,
            )

        val exporter = FfmpegVideoExporter(videoPath = { File(it.file).toOkioPath() }, fonts = { OverlayFonts.Default })
        val last = runBlocking {
            exporter.export(item, Quality.UHD_4K, withSound = true, out = out.toOkioPath()).last()
        }
        assertTrue(last is ExportProgress.Done, "export did not finish: $last")
        println("HeaderMasterTest: ${out.absolutePath} ${out.length() / 1_000_000} MB")
    }
}
