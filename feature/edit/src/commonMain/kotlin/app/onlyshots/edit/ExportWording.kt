package app.onlyshots.edit

import androidx.compose.runtime.staticCompositionLocalOf
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.edit_desktop_footer
import app.onlyshots.core.resources.edit_desktop_save
import app.onlyshots.core.resources.edit_desktop_save_where
import app.onlyshots.core.resources.edit_desktop_saved
import app.onlyshots.core.resources.edit_desktop_share
import app.onlyshots.core.resources.edit_desktop_share_where
import app.onlyshots.core.resources.edit_desktop_shared
import app.onlyshots.core.resources.edit_export_leave
import app.onlyshots.core.resources.edit_export_leave_web
import app.onlyshots.core.resources.edit_export_notify_text
import app.onlyshots.core.resources.edit_export_notify_text_web
import app.onlyshots.core.resources.edit_export_open_gallery
import app.onlyshots.core.resources.edit_open_another
import app.onlyshots.core.resources.edit_phone_footer
import app.onlyshots.core.resources.edit_phone_next
import app.onlyshots.core.resources.edit_phone_save
import app.onlyshots.core.resources.edit_phone_save_where
import app.onlyshots.core.resources.edit_phone_saved
import app.onlyshots.core.resources.edit_web_footer
import app.onlyshots.core.resources.edit_web_save
import app.onlyshots.core.resources.edit_web_save_where
import app.onlyshots.core.resources.edit_web_saved
import org.jetbrains.compose.resources.StringResource

/**
 * What the export sheet calls its two ways out, per host: the phones' gallery and share sheet, the desktop's Movies
 * folder and Finder, the browser's download. A null [share] leaves that row out.
 */
data class ExportWording(
    val save: StringResource,
    val saveWhere: StringResource,
    val share: StringResource?,
    val shareWhere: StringResource? = null,
    /** Where the video is once [save] finished. */
    val saved: StringResource,
    /** What happened once [share] finished; null says nothing. */
    val shared: StringResource? = null,
    val next: StringResource,
    val footer: StringResource,
    /** Whether this host's export carries sound at all. */
    val sound: Boolean = true,
    /** Under a running export, where it goes on outside the app: in place of [footer]; null keeps the footer. */
    val leave: StringResource? = null,
    /** The first export's offer to say when it is done; null offers nothing. */
    val notifyText: StringResource? = null,
    /** The done state's key to the video in the gallery's app; null where there is none. */
    val openSaved: StringResource? = null,
) {
    companion object {
        val Phone =
            ExportWording(
                save = Res.string.edit_phone_save,
                saveWhere = Res.string.edit_phone_save_where,
                // Saved to the gallery only: it is shared from there.
                share = null,
                saved = Res.string.edit_phone_saved,
                next = Res.string.edit_phone_next,
                footer = Res.string.edit_phone_footer,
                leave = Res.string.edit_export_leave,
                notifyText = Res.string.edit_export_notify_text,
                openSaved = Res.string.edit_export_open_gallery,
            )

        val Desktop =
            ExportWording(
                save = Res.string.edit_desktop_save,
                saveWhere = Res.string.edit_desktop_save_where,
                share = Res.string.edit_desktop_share,
                shareWhere = Res.string.edit_desktop_share_where,
                saved = Res.string.edit_desktop_saved,
                shared = Res.string.edit_desktop_shared,
                next = Res.string.edit_open_another,
                footer = Res.string.edit_desktop_footer,
            )

        /** The browser's export is 1080p, and its one way out is the download. */
        val Web =
            ExportWording(
                save = Res.string.edit_web_save,
                saveWhere = Res.string.edit_web_save_where,
                share = null,
                saved = Res.string.edit_web_saved,
                next = Res.string.edit_open_another,
                footer = Res.string.edit_web_footer,
                leave = Res.string.edit_export_leave_web,
                notifyText = Res.string.edit_export_notify_text_web,
            )
    }
}

val LocalExportWording = staticCompositionLocalOf { ExportWording.Phone }
