package app.onlyshots

import androidx.compose.ui.Modifier

/**
 * Stable names for the controls and screens UI tests drive (`:uitests`, the Android UI Automator and iOS XCUITest smoke
 * tests, where they appear as resource ids and accessibility identifiers). A tag names what a node does, never how it
 * looks, so a redesign keeps them. Rename one only together with its tests.
 */
object TestTags {
    /** The one back button, on every screen that has a way back. */
    const val Back = "back"

    // Screens: the root of each destination.
    const val ScreenStart = "screen.start"
    const val ScreenHome = "screen.home"
    const val ScreenRecord = "screen.record"
    const val ScreenOpen = "screen.open"
    const val ScreenEdit = "screen.edit"
    const val ScreenPrivacy = "screen.privacy"
    const val ScreenMachineShots = "screen.machineShots"
    const val ScreenStyleEditor = "screen.styleEditor"

    // Start.
    const val StartRecord = "start.record"
    const val StartCompose = "start.compose"
    const val StartHelp = "start.help"
    const val HelpPrivacy = "help.privacy"

    // Home.
    const val HomeSample = "home.sample"
    const val HomeRecordAction = "home.recordAction"
    const val HomeAddByAddress = "home.addByAddress"
    const val HomeAddressField = "home.addressField"
    const val HomeAddressConnect = "home.addressConnect"

    // Record.
    const val RecordButton = "record.button"
    const val RecordSimulate = "record.simulate"
    const val RecordSettings = "record.settings"
    const val RecordStatus = "record.status"
    const val RecordAllowCamera = "record.allowCamera"

    // Open.
    const val OpenPickVideo = "open.pickVideo"
    const val OpenPickShot = "open.pickShot"
    const val OpenFromFile = "open.fromFile"
    const val OpenFromMachine = "open.fromMachine"
    const val OpenEditor = "open.editor"

    // Edit.
    const val EditSound = "edit.sound"
    const val EditUndo = "edit.undo"
    const val EditRedo = "edit.redo"
    const val EditExport = "edit.export"
    const val EditShotChip = "edit.shotChip"
    /** The graph over the picture, in the editor and on the record screen: tap to select. */
    const val Graph = "graph"
    const val EditTimeline = "edit.timeline"
    const val EditFrame = "edit.frame"
    const val ToolbarStyle = "edit.toolbar.style"
    const val ToolbarData = "edit.toolbar.data"
    const val ExportSave = "export.save"
    const val ExportShare = "export.share"
    const val ExportKeepEditing = "export.keepEditing"
    const val ExportNotifyAllow = "export.notify.allow"
    const val ExportNotifyLater = "export.notify.later"
    const val ExportOpenSaved = "export.openSaved"
    const val ExportNext = "export.next"
    const val TextTitleField = "edit.text.title"

    /** One video format in the frame picker: `format(VideoFormat.SQUARE.name)`. */
    fun format(name: String) = "edit.format.$name"

    /** One series chip in the data sheet: `series(Reading.PRESSURE.name)`. */
    fun series(name: String) = "edit.series.$name"

    // Sheets and dialogs.
    const val SheetDone = "sheet.done"
    const val DialogConfirm = "dialog.confirm"
    const val DialogDismiss = "dialog.dismiss"

    // Style picker and editor.
    const val StyleNew = "style.new"
    const val StyleEditorSave = "styleEditor.save"
    const val StyleSaveConfirm = "styleEditor.saveConfirm"
    const val StyleMenuEdit = "style.menu.edit"
    const val StyleMenuCopy = "style.menu.copy"
    const val StyleMenuDelete = "style.menu.delete"
    const val StyleNameField = "style.nameField"

    /** The selected style's keys under "your styles". */

    /** One choice of a segmented setting: `choice("grid", GridMode.ROWS.name)`. */
    fun choice(group: String, value: String) = "choice.$group.$value"

    /** One style tile in the picker, by the style's id. */
    fun styleTile(id: String) = "style.tile.$id"

    /** One style editor tab: `styleTab("panel")`. */
    fun styleTab(name: String) = "styleEditor.tab.$name"
}

/**
 * Shows the test tags under this node to UI Automator as resource ids. Each app root sets it; a dialog, sheet or menu
 * is a window of its own and doesn't inherit it, so each sets it again. Android only: iOS shows tags as accessibility
 * identifiers by itself.
 */
expect fun Modifier.testTagsAsResourceIds(): Modifier
