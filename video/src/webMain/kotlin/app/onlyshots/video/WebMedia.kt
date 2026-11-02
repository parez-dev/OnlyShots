package app.onlyshots.video

import kotlin.js.Promise
import kotlinx.coroutines.await
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

/**
 * The web editor's file system: sidecars, settings, thumbnails and picked shot files, in memory for the session. The
 * videos themselves stay browser `File`s and `Blob`s, which `media.js` keeps by the same paths (see [WebMedia]).
 */
object WebFiles {
    val fs: FileSystem = FakeFileSystem()
}

/** What [WebMedia.probe] reads from a file. */
class WebProbe(val durationUs: Long, val hasAudio: Boolean, val width: Int, val height: Int, val createdMs: Double)

/**
 * `media.js` (globalThis.cg) from Kotlin: the browser's files, playback, decoding and encoding. Video frames are
 * written straight into Skia's memory and read straight out of it; smaller things (thumbnails, sound, luma) cross in
 * one copy each way ([bytesOf], [floatsOf], [floatsToJs]: through Kotlin's wasm memory, or as typed arrays on JS).
 */
object WebMedia {
    fun register(path: String, blob: JsAny) = cgRegister(path, blob)

    /** Makes the file at [from] reachable at [to] as well, without copying it; false if there is none at [from]. */
    fun alias(from: String, to: String): Boolean = cgAlias(from, to)

    fun forget(path: String) = cgForget(path)

    /** Opens the browser's file dialog; the path the picked file is registered under, or null. */
    suspend fun pickFile(accept: String): String? = cgPickFile(accept).await<JsString?>()?.toString()

    fun onDrop(onFile: (String) -> Unit, onHover: (Boolean) -> Unit) = cgOnDrop(onFile, onHover)

    suspend fun readText(path: String): String = cgReadText(path).await<JsString>().toString()

    fun download(path: String, name: String) = cgDownload(path, name)

    suspend fun probe(path: String): WebProbe {
        val p = cgProbe(path).await<JsAny>()
        if (!bool(p, "canDecode")) throw UndecodableVideoException(str(p, "codec"))
        return WebProbe(
            durationUs = (num(p, "duration") * 1_000_000).toLong(),
            hasAudio = bool(p, "hasAudio"),
            width = num(p, "width").toInt(),
            height = num(p, "height").toInt(),
            createdMs = num(p, "created"),
        )
    }

    suspend fun thumbnailJpeg(path: String, atSeconds: Double, maxWidth: Int): ByteArray =
        bytesOf(cgThumbnail(path, atSeconds, maxWidth).await<JsAny>())

    suspend fun decodeAudio(path: String, maxSeconds: Int): MonoPcm? {
        val r = cgDecodeAudio(path, maxSeconds).await<JsAny?>() ?: return null
        return MonoPcm(floatsOf(field(r, "samples")), num(r, "rate").toInt())
    }

    suspend fun lumaAt(path: String, atSeconds: Double, points: List<Pair<Float, Float>>): List<Float>? {
        val flat =
            FloatArray(points.size * 2).also { a ->
                points.forEachIndexed { i, (x, y) ->
                    a[2 * i] = x
                    a[2 * i + 1] = y
                }
            }
        val r = cgLumaAt(path, atSeconds, floatsToJs(flat)).await<JsAny?>() ?: return null
        return floatsOf(r).toList()
    }

    fun playerOpen(path: String, maxSide: Int): Int = cgPlayerOpen(path, maxSide)

    fun playerClose(id: Int) = cgPlayerClose(id)

    suspend fun playerSeek(id: Int, seconds: Double) {
        cgPlayerSeek(id, seconds).await<JsAny?>()
    }

    suspend fun playerPlay(id: Int) {
        cgPlayerPlay(id).await<JsAny?>()
    }

    fun playerPause(id: Int) = cgPlayerPause(id)

    fun playerTime(id: Int): Double = cgPlayerTime(id)

    fun playerMute(id: Int, muted: Boolean) = cgPlayerMute(id, muted)

    /**
     * A new frame as a Skia bitmap, written by the browser straight into Skia's memory; null when nothing changed since
     * the last call.
     */
    fun playerFrame(id: Int): Bitmap? {
        if (!cgPlayerFrame(id)) return null
        val bitmap = Bitmap()
        bitmap.allocPixels(
            ImageInfo(cgPlayerWidth(id), cgPlayerHeight(id), ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
        )
        cgPlayerPixelsInto(id, skiaMemory(), pixelAddress(bitmap))
        bitmap.setImmutable()
        return bitmap
    }

    /** Starts an export of [path] between the two times; resolves with the source's frame rate. */
    suspend fun exportStart(
        path: String,
        width: Int,
        height: Int,
        startSeconds: Double,
        endSeconds: Double,
        zoom: Double,
        offsetX: Double,
        offsetY: Double,
        withSound: Boolean,
    ): Double {
        if (!hasVideoEncoder()) throw NoWebCodecsException()
        return cgExportStart(path, width, height, startSeconds, endSeconds, zoom, offsetX, offsetY, withSound)
            .await<JsNumber>()
            .toDouble()
    }

    /** The next decoded frame's time in seconds, or a negative number at the end. */
    suspend fun exportNext(): Double = cgExportNext().await<JsNumber>().toDouble()

    /** Writes the decoded frame into [bitmap]'s pixels (RGBA, the export's size). */
    fun exportFrameInto(bitmap: Bitmap) = cgExportFrameInto(skiaMemory(), pixelAddress(bitmap))

    /** Encodes [bitmap] (RGBA, the export's size) at [timestamp] seconds. */
    suspend fun exportAddFrom(bitmap: Bitmap, timestamp: Double, duration: Double) {
        cgExportAddFrom(skiaMemory(), pixelAddress(bitmap), timestamp, duration).await<JsAny?>()
    }

    private fun pixelAddress(bitmap: Bitmap): Int = (bitmap.peekPixels() ?: error("no pixels")).addr

    suspend fun exportFinish(outPath: String) {
        cgExportFinish(outPath).await<JsAny?>()
    }

    suspend fun exportCancel() {
        cgExportCancel().await<JsAny?>()
    }
}

private fun cgRegister(path: String, blob: JsAny): Unit = js("globalThis.cg.register(path, blob)")

private fun cgAlias(from: String, to: String): Boolean = js("globalThis.cg.alias(from, to)")

private fun cgForget(path: String): Unit = js("globalThis.cg.forget(path)")

private fun cgPickFile(accept: String): Promise<JsString?> = js("globalThis.cg.pickFile(accept)")

private fun cgOnDrop(onFile: (String) -> Unit, onHover: (Boolean) -> Unit): Unit =
    js("globalThis.cg.onDrop(onFile, onHover)")

private fun cgReadText(path: String): Promise<JsString> = js("globalThis.cg.readText(path)")

private fun cgDownload(path: String, name: String): Unit = js("globalThis.cg.download(path, name)")

private fun cgProbe(path: String): Promise<JsAny> = js("globalThis.cg.probe(path)")

private fun cgThumbnail(path: String, time: Double, maxWidth: Int): Promise<JsAny> =
    js("globalThis.cg.thumbnail(path, time, maxWidth)")

private fun cgDecodeAudio(path: String, maxSeconds: Int): Promise<JsAny?> =
    js("globalThis.cg.decodeAudio(path, maxSeconds)")

private fun cgLumaAt(path: String, time: Double, points: JsAny): Promise<JsAny?> =
    js("globalThis.cg.lumaAt(path, time, points)")

private fun cgPlayerOpen(path: String, maxSide: Int): Int = js("globalThis.cg.playerOpen(path, maxSide)")

private fun cgPlayerClose(id: Int): Unit = js("globalThis.cg.playerClose(id)")

private fun cgPlayerSeek(id: Int, time: Double): Promise<JsAny?> = js("globalThis.cg.playerSeek(id, time)")

private fun cgPlayerPlay(id: Int): Promise<JsAny?> = js("globalThis.cg.playerPlay(id)")

private fun cgPlayerPause(id: Int): Unit = js("globalThis.cg.playerPause(id)")

private fun cgPlayerTime(id: Int): Double = js("globalThis.cg.playerTime(id)")

private fun cgPlayerMute(id: Int, muted: Boolean): Unit = js("globalThis.cg.playerMute(id, muted)")

private fun cgPlayerFrame(id: Int): Boolean = js("globalThis.cg.playerFrame(id)")

private fun cgPlayerPixelsInto(id: Int, memory: JsAny, address: Int): Unit =
    js("globalThis.cg.playerPixelsInto(id, memory, address)")

private fun cgPlayerWidth(id: Int): Int = js("globalThis.cg.playerWidth(id)")

private fun cgPlayerHeight(id: Int): Int = js("globalThis.cg.playerHeight(id)")

private fun cgExportStart(
    path: String,
    w: Int,
    h: Int,
    start: Double,
    end: Double,
    zoom: Double,
    offsetX: Double,
    offsetY: Double,
    sound: Boolean,
): Promise<JsNumber> = js("globalThis.cg.exportStart(path, w, h, start, end, zoom, offsetX, offsetY, sound)")

private fun cgExportNext(): Promise<JsNumber> = js("globalThis.cg.exportNext()")

private fun cgExportFrameInto(memory: JsAny, address: Int): Unit = js("globalThis.cg.exportFrameInto(memory, address)")

private fun cgExportAddFrom(memory: JsAny, address: Int, timestamp: Double, duration: Double): Promise<JsAny?> =
    js("globalThis.cg.exportAddFrom(memory, address, timestamp, duration)")

private fun cgExportFinish(outPath: String): Promise<JsAny?> = js("globalThis.cg.exportFinish(outPath)")

private fun cgExportCancel(): Promise<JsAny?> = js("globalThis.cg.exportCancel()")

private fun hasVideoEncoder(): Boolean = js("typeof VideoEncoder !== 'undefined'")

private fun num(o: JsAny, key: String): Double = js("Number(o[key] || 0)")

private fun bool(o: JsAny, key: String): Boolean = js("!!o[key]")

/** A field as text; a missing one reads empty, which [field] could not do without trapping on wasm. */
private fun str(o: JsAny, key: String): String = js("String(o[key] || '')")

private fun field(o: JsAny, key: String): JsAny = js("o[key]")
