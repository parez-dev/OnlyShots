package app.onlyshots.di

import app.onlyshots.camera.CameraController
import app.onlyshots.camera.IosCameraController
import app.onlyshots.core.designsystem.loadOverlayFonts
import app.onlyshots.editor.CACHE_DIR
import app.onlyshots.editor.LIBRARY_DIR
import app.onlyshots.platform.BackgroundTaskWork
import app.onlyshots.platform.BackgroundWork
import app.onlyshots.platform.IosGallery
import app.onlyshots.platform.PlatformShare
import app.onlyshots.platform.VideoGallery
import app.onlyshots.storage.LibraryStore
import app.onlyshots.video.AvFoundationVideoExporter
import app.onlyshots.video.AvFoundationVideoProbe
import app.onlyshots.video.FrameSampler
import app.onlyshots.video.IosFrameSampler
import app.onlyshots.video.VideoExporter
import app.onlyshots.video.VideoProbe
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

private fun directory(kind: ULong): Path =
    (NSSearchPathForDirectoriesInDomains(kind, NSUserDomainMask, true).first() as String).toPath()

actual val platformModule: Module = module {
    single<FileSystem> { FileSystem.SYSTEM }
    single<Path>(named(CACHE_DIR)) { directory(NSCachesDirectory) }
    single<Path>(named(LIBRARY_DIR)) { directory(NSDocumentDirectory) / "library" }
    single<CameraController> { IosCameraController((directory(NSCachesDirectory) / "camera").toString()) }
    single<VideoExporter> {
        val videosDir = get<LibraryStore>().videosDir
        AvFoundationVideoExporter(
            videoPath = { item -> if (item.file.startsWith("/")) item.file.toPath() else videosDir / item.file },
            fonts = { loadOverlayFonts() },
        )
    }
    single<FrameSampler> { IosFrameSampler() }
    single<VideoProbe> { AvFoundationVideoProbe() }
    single<PlatformShare> { IosShare() }
    single<BackgroundWork> { BackgroundTaskWork() }
    single<VideoGallery> { IosGallery() }
}

private class IosShare : PlatformShare {
    override fun shareVideos(files: List<Path>, onDone: () -> Unit) {
        if (files.isEmpty()) return
        // From whatever is in front (the native shell's bar, a picker), not the window's root under it.
        var root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        while (true) root = root.presentedViewController ?: break
        val sheet = UIActivityViewController(files.map { NSURL.fileURLWithPath(it.toString()) }, null)
        // iPad shows the sheet as a popover, which needs an anchor.
        sheet.popoverPresentationController?.sourceView = root.view
        sheet.setCompletionWithItemsHandler { _, _, _, _ -> onDone() }
        root.presentViewController(sheet, animated = true, completion = null)
    }
}
