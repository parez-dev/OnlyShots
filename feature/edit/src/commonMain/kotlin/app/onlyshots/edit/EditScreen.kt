package app.onlyshots.edit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.onlyshots.SharedKeys
import app.onlyshots.StyleEditorRoute
import app.onlyshots.TestTags
import app.onlyshots.core.designsystem.CgColors
import app.onlyshots.core.designsystem.CgIcons
import app.onlyshots.core.designsystem.CgMotion
import app.onlyshots.core.designsystem.CgMotion.cut
import app.onlyshots.core.designsystem.CgSize
import app.onlyshots.core.designsystem.CgSpace
import app.onlyshots.core.designsystem.CgStroke
import app.onlyshots.core.designsystem.CgTheme
import app.onlyshots.core.designsystem.caps
import app.onlyshots.core.designsystem.component.BackButton
import app.onlyshots.core.designsystem.component.Banner
import app.onlyshots.core.designsystem.component.BannerKind
import app.onlyshots.core.designsystem.component.BarIcon
import app.onlyshots.core.designsystem.component.BarPriority
import app.onlyshots.core.designsystem.component.CgButton
import app.onlyshots.core.designsystem.component.CgDialog
import app.onlyshots.core.designsystem.component.CgIcon
import app.onlyshots.core.designsystem.component.DialogRisk
import app.onlyshots.core.designsystem.component.EditorSheet
import app.onlyshots.core.designsystem.component.EditorSheetSlot
import app.onlyshots.core.designsystem.component.ExportAsk
import app.onlyshots.core.designsystem.component.ExportOverlay
import app.onlyshots.core.designsystem.component.ExportSavedKeys
import app.onlyshots.core.designsystem.component.FormatOption
import app.onlyshots.core.designsystem.component.FormatPicker
import app.onlyshots.core.designsystem.component.LocalNativeChrome
import app.onlyshots.core.designsystem.component.NativeBarItem
import app.onlyshots.core.designsystem.component.NativeItems
import app.onlyshots.core.designsystem.component.OVER_VIDEO
import app.onlyshots.core.designsystem.component.Pill
import app.onlyshots.core.designsystem.component.SegmentedControl
import app.onlyshots.core.designsystem.component.StatusChip
import app.onlyshots.core.designsystem.component.TopChip
import app.onlyshots.core.designsystem.component.TwoPane
import app.onlyshots.core.designsystem.component.topRule
import app.onlyshots.core.model.Shot
import app.onlyshots.core.model.SidecarCodec
import app.onlyshots.core.model.VideoFormat
import app.onlyshots.core.model.VideoItem
import app.onlyshots.core.model.style.GraphStyle
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.UiText
import app.onlyshots.core.resources.edit_add_shot
import app.onlyshots.core.resources.edit_change
import app.onlyshots.core.resources.edit_data
import app.onlyshots.core.resources.edit_done
import app.onlyshots.core.resources.edit_export
import app.onlyshots.core.resources.edit_export_notify_allow
import app.onlyshots.core.resources.edit_export_notify_later
import app.onlyshots.core.resources.edit_export_notify_title
import app.onlyshots.core.resources.edit_find_start
import app.onlyshots.core.resources.edit_fit
import app.onlyshots.core.resources.edit_frame
import app.onlyshots.core.resources.edit_frame_switch
import app.onlyshots.core.resources.edit_gallery_access
import app.onlyshots.core.resources.edit_graph
import app.onlyshots.core.resources.edit_graph_start
import app.onlyshots.core.resources.edit_keep_editing
import app.onlyshots.core.resources.edit_leave
import app.onlyshots.core.resources.edit_leave_text
import app.onlyshots.core.resources.edit_leave_title
import app.onlyshots.core.resources.edit_loading
import app.onlyshots.core.resources.edit_making_video
import app.onlyshots.core.resources.edit_match_by_clock
import app.onlyshots.core.resources.edit_muted
import app.onlyshots.core.resources.edit_of_duration
import app.onlyshots.core.resources.edit_ok
import app.onlyshots.core.resources.edit_opacity
import app.onlyshots.core.resources.edit_open_the_phone
import app.onlyshots.core.resources.edit_pause
import app.onlyshots.core.resources.edit_phases_count
import app.onlyshots.core.resources.edit_play
import app.onlyshots.core.resources.edit_recorded_with_video
import app.onlyshots.core.resources.edit_redo
import app.onlyshots.core.resources.edit_reframe_hint
import app.onlyshots.core.resources.edit_saved
import app.onlyshots.core.resources.edit_shot
import app.onlyshots.core.resources.edit_sound
import app.onlyshots.core.resources.edit_start_here
import app.onlyshots.core.resources.edit_step_back
import app.onlyshots.core.resources.edit_step_forward
import app.onlyshots.core.resources.edit_style
import app.onlyshots.core.resources.edit_style_named
import app.onlyshots.core.resources.edit_tap_graph_hint
import app.onlyshots.core.resources.edit_undo
import app.onlyshots.core.resources.edit_zoom_and_crop
import app.onlyshots.core.resources.edit_zoom_in
import app.onlyshots.core.resources.text
import app.onlyshots.export.ExportManager
import app.onlyshots.label
import app.onlyshots.layout.LayoutKind
import app.onlyshots.layout.LocalWindowLayout
import app.onlyshots.layout.WidePanes
import app.onlyshots.layout.animateSizeOn
import app.onlyshots.layout.belowPanelHeightDp
import app.onlyshots.layout.fitRatio
import app.onlyshots.layout.placePanel
import app.onlyshots.layout.sidePanelWidthDp
import app.onlyshots.machineLabel
import app.onlyshots.overlay.compose.ShotOverlay
import app.onlyshots.overlayTitleText
import app.onlyshots.platform.rememberGalleryAccess
import app.onlyshots.platform.rememberNotificationPermission
import app.onlyshots.platform.rememberShotFilePicker
import app.onlyshots.shareBounds
import app.onlyshots.style.StylePicker
import app.onlyshots.style.StylePickerActions
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The editor, laid out as the web editor's phone layout: chips for the shot and the export along the top, the picture
 * filling the screen, and the timeline band under it; everything else in sheets that leave the picture in view.
 *
 * Tap the picture to play or pause; tap the graph to select it, which brings up its outline, its grip and a toolbar for
 * style, data, transparency and a reset. Tap the picture again to put the graph or a sheet down.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditScreen(
    id: String,
    onBack: () -> Unit,
    onExported: () -> Unit,
    onStyleEditor: (StyleEditorRoute) -> Unit = {},
    savedStyle: StateFlow<String?> = MutableStateFlow(null),
    onSavedStyleUsed: () -> Unit = {},
    /** Whether leaving a video that was never saved or shared asks first: on a phone it cannot be opened again. */
    askBeforeLeaving: Boolean = true,
    /** Opens the phones' machine shot picker; null where the host cannot reach machines (desktop, web). */
    onFromMachine: (() -> Unit)? = null,
    /** A shot the machine shot picker loaded, as JSON, to attach; [onPickedShotUsed] once it is. */
    pickedShot: StateFlow<String?> = MutableStateFlow(null),
    onPickedShotUsed: () -> Unit = {},
    viewModel: EditViewModel = koinViewModel { parametersOf(id) },
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val customStyles by viewModel.styles.customStyles.collectAsStateWithLifecycle(viewModel.styles.custom)
    // A style just saved in the style editor becomes this video's style.
    val justSaved by savedStyle.collectAsStateWithLifecycle()
    LaunchedEffect(justSaved) {
        justSaved?.let {
            viewModel.pickStyle(it)
            onSavedStyleUsed()
        }
    }
    val picked by pickedShot.collectAsStateWithLifecycle()
    LaunchedEffect(picked, ui.item != null) {
        val json = picked ?: return@LaunchedEffect
        if (ui.item == null) return@LaunchedEffect
        runCatching { SidecarCodec.json.decodeFromString(Shot.serializer(), json) }
            .getOrNull()
            ?.let(viewModel::attachShot)
        onPickedShotUsed()
    }
    val player = remember { PlayerState() }
    // A Gaggiuino shot the machine uploaded on its own: SproFiler, the visualizer and the machine's own API all hand
    // out the same file.
    val pickShotFile = rememberShotFilePicker(viewModel::importShotFile)
    val video = ui.item
    // Sound unless the take is muted.
    val sound = !player.muted && video?.hasAudio == true
    // The gallery may need a permission first (Photos on iOS, storage on old Android): asked on the tap, not before.
    val askGallery = rememberGalleryAccess { granted ->
        if (granted) viewModel.saveAndExport(ui.galleryQuality, sound, ExportManager.Destination.GALLERY)
        else viewModel.showMessage(Res.string.edit_gallery_access.text())
    }
    // The playhead starts where the export does, and stays inside the trim when the trim moves past it.
    LaunchedEffect(video?.trimInUs, video?.trimOutUs) {
        val v = video ?: return@LaunchedEffect
        if (player.positionUs !in v.trimInUs..v.trimOutUs)
            player.seek(player.positionUs.coerceIn(v.trimInUs, v.trimOutUs))
    }

    // There is no library to come back to: leaving a video that was never saved or shared asks first, from the back
    // button and the system back alike. An open sheet is put down before anything else.
    var askLeave by remember { mutableStateOf(false) }
    val leave = { if (ui.kept || !askBeforeLeaving) onBack() else askLeave = true }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None)) {
        if (ui.sheet != null && ui.exporting == null) viewModel.closeSheet() else leave()
    }
    if (askLeave) {
        CgDialog(
            title = stringResource(Res.string.edit_leave_title),
            text = stringResource(Res.string.edit_leave_text),
            onDismiss = { askLeave = false },
            confirm = stringResource(Res.string.edit_keep_editing) to { askLeave = false },
            risk = DialogRisk.Dismiss,
            dismiss =
                stringResource(Res.string.edit_leave) to
                    {
                        askLeave = false
                        onBack()
                    },
        )
    }

    // The sheet closes as the picker opens, so coming back shows the video with its new shot.
    val fromMachine = onFromMachine?.let { open ->
        {
            viewModel.closeSheet()
            open()
        }
    }
    CompositionLocalProvider(LocalFromMachine provides fromMachine) {
        EditBody(
            ui = ui,
            customStyles = customStyles,
            player = player,
            actions = viewModel,
            onBack = leave,
            onStyleEditor = onStyleEditor,
            onExported = onExported,
            onPickShotFile = pickShotFile,
            onSaveToGallery = askGallery,
            picture = { modifier ->
                val v = ui.item
                val path = viewModel.videoPath
                if (v != null && path != null) {
                    VideoSurface(
                        file = path,
                        state = player,
                        clipUs = v.trimInUs..v.trimOutUs,
                        drawFrames = ui.style?.blursVideo == true,
                        modifier = modifier,
                    )
                }
            },
        )
    }
}

/**
 * The editor drawn from plain state: [ui], the user's [customStyles], the [player]'s clock, and [actions] to ask for
 * changes. [picture] draws the video into the modifier it is given (the platform's video surface in the app, a still in
 * a screenshot). Phone layout, or on a wide window the picture with its timeline beside the panel.
 */
@Composable
fun EditBody(
    ui: EditUiState,
    customStyles: List<GraphStyle>,
    player: PlayerState,
    actions: EditActions,
    onBack: () -> Unit,
    onStyleEditor: (StyleEditorRoute) -> Unit,
    onExported: () -> Unit,
    onPickShotFile: () -> Unit,
    onSaveToGallery: () -> Unit,
    picture: @Composable (Modifier) -> Unit,
    initiallySelected: Boolean = false,
    initiallyReframing: Boolean = false,
) {
    var graphSelected by remember { mutableStateOf(initiallySelected) }
    var reframing by remember { mutableStateOf(initiallyReframing) }
    var zoom by remember { mutableStateOf<Float?>(null) }
    var lastPage by remember { mutableStateOf(EditSheet.STYLE) }
    val setReframing = { on: Boolean ->
        reframing = on
        if (on) graphSelected = false
    }
    val video = ui.item
    val wide = LocalWindowLayout.current.kind == LayoutKind.Wide && video != null
    // On a wide window the side panel holds the graph's settings while the graph is selected, the video's otherwise.
    val graphPanel = graphSelected && video?.shot != null && !reframing
    // The export straight from the top bar on a wide window, or from the keyboard.
    val export = { if (video != null && ui.exporting == null) onSaveToGallery() }
    // Cmd/Ctrl+Z, Cmd/Ctrl+Shift+Z and Cmd/Ctrl+E on a keyboard, as any editor.
    val focus = remember { FocusRequester() }
    // On a wide window the export is an overlay over everything, while it runs and until its result is seen.
    val exportDone = ui.savedToGallery || ui.exported
    val overlay = wide && (ui.exporting != null || exportDone)
    // On a phone it takes the whole screen once it runs, until the saved video is seen to (shared, it goes on at once).
    val phoneOverlay = !wide && video != null && (ui.exporting != null || ui.savedToGallery)
    val keys =
        Modifier.focusRequester(focus)
            .focusable()
            .editorKeys(
                actions,
                export,
                blocked = { overlay || phoneOverlay },
                onEscape = {
                    when {
                        overlay -> if (exportDone) actions.acknowledgeExport()
                        graphSelected -> graphSelected = false
                    }
                },
            )
    // Exported and handed to the share sheet: on a phone, on to the next shot; a wide window stays and says so.
    LaunchedEffect(ui.exported, wide) { if (ui.exported && !wide) onExported() }
    // Once the layout holding [keys] is in place: the phone layout until the video loads, then perhaps the wide one.
    LaunchedEffect(wide) {
        withFrameNanos {}
        focus.requestFocus()
    }
    val sound = !player.muted && video?.hasAudio == true
    val share = { actions.saveAndExport(ui.shareQuality, sound) }
    val leave = onBack
    // What the sheets hold: a bottom sheet on a phone, the side panel on a wide window.
    val sheetBody: @Composable (EditSheet, VideoItem) -> Unit = { sheet, video ->
        when (sheet) {
            EditSheet.STYLE ->
                StylePicker(
                    currentId = ui.style?.id ?: video.overlay.styleId,
                    custom = customStyles,
                    actions =
                        StylePickerActions(
                            pick = actions::pickStyle,
                            create = { base -> onStyleEditor(StyleEditorRoute(baseId = base, videoId = video.id)) },
                            edit = { sid -> onStyleEditor(StyleEditorRoute(editId = sid, videoId = video.id)) },
                            rename = { sid, name -> actions.renameStyle(sid, name) },
                            delete = actions::deleteStyle,
                        ),
                )
            // The graph's words go with its data: what it shows and what it says, in one place.
            EditSheet.DATA -> {
                DataSheetContent(
                    video.overlay,
                    seriesIn(video.shot),
                    hasPhases = video.shot?.phaseStartsUs?.isNotEmpty() == true,
                ) { change ->
                    actions.overlay(change = change)
                }
                GraphTextFields(video) { push, change -> actions.overlay(push, change) }
            }
            EditSheet.SHOT ->
                ShotSheetContent(
                    video,
                    ui.shots,
                    onPick = actions::pickShot,
                    onFile = onPickShotFile,
                    onMatchByClock = actions::matchByClock,
                    onUnlock = actions::unlockShotStart,
                    onFromMachine = LocalFromMachine.current,
                )
            EditSheet.EXPORT ->
                ExportSheetContent(
                    item = video,
                    galleryQuality = ui.galleryQuality,
                    shareQuality = ui.shareQuality,
                    qualities = ui.qualities,
                    sound = sound,
                    exporting = ui.exporting,
                    leftMs = ui.exportLeftMs,
                    savedToGallery = ui.savedToGallery,
                    onShare = share,
                    onSaveToGallery = onSaveToGallery,
                    onNextShot = onExported,
                    onCancel = actions::cancelExport,
                    onQuality = actions::pickQuality,
                )
        }
    }

    val layout = LocalWindowLayout.current
    // The way out, at the bottom of a wide editor's panel whichever page it shows.
    val exportBar =
        @Composable {
            if (video != null) {
                ExportBar(
                    video,
                    galleryQuality = ui.galleryQuality,
                    shareQuality = ui.shareQuality,
                    qualities = ui.qualities,
                    sound = sound,
                    onSave = export,
                    onShare = share,
                    onQuality = actions::pickQuality,
                )
            }
        }
    EditNativeBar(ui, actions, player, onExport = if (layout.kind == LayoutKind.Phone) null else export)
    // A flip's cover screen: there is no room to edit, only to say so.
    if (layout.kind == LayoutKind.Tiny) {
        Box(Modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding().then(keys)) {
            BackButton(onClick = leave, modifier = Modifier.align(Alignment.TopStart).padding(CgSpace.S16))
            Text(
                stringResource(Res.string.edit_open_the_phone),
                style = CgTheme.type.screenTitle,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.align(Alignment.Center).padding(start = CgSpace.S24, end = CgSpace.S24, bottom = 76.dp),
            )
        }
        return
    }
    // On its side (a book in tabletop, a flip in flex mode): the player in the upper half, the timeline and the
    // tools on the lower one.
    if (layout.kind == LayoutKind.Stacked && video != null) {
        Box(Modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding().then(keys)) {
            TwoPane(
                vertical = true,
                splitDp = layout.splitDp.dp,
                hingeDp = layout.hingeDp.dp,
                first = {
                    PlayerHalf(
                        ui = ui,
                        video = video,
                        player = player,
                        actions = actions,
                        onBack = leave,
                        reframing = reframing,
                        onReframing = setReframing,
                    ) { modifier ->
                        Picture(
                            ui = ui,
                            video = video,
                            player = player,
                            actions = actions,
                            picture = picture,
                            selected = graphSelected,
                            onSelected = { graphSelected = it },
                            reframing = reframing,
                            toolbar = false,
                            modifier = modifier,
                        )
                    }
                },
                second = {
                    Column(Modifier.fillMaxSize()) {
                        TimeBand(video, ui, player, zoom, onZoom = { zoom = it }, actions)
                        ToolChips(ui, video, actions, onExport = export)
                    }
                },
            )
            EditorSheetSlot(
                ui.sheet,
                Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.72f).wrapContentHeight(Alignment.Bottom),
            ) { sheet ->
                EditorSheet(
                    title = sheetTitle(sheet, ui),
                    onDone = actions::closeSheet.takeIf { ui.exporting == null },
                ) {
                    sheetBody(sheet, video)
                }
            }
        }
        return
    }
    if (layout.kind == LayoutKind.Wide && video != null) {
        // As the web editor's desk: the picture with its timeline, and everything else in a panel beside it.
        Box(Modifier.fillMaxSize().background(CgColors.Surface.Base).then(keys)) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                // The panel beside the picture, or under it across the window where that leaves the video more room (a
                // wide
                // or square video on an upright tablet); picking another format moves it there animated.
                val placement =
                    placePanel(
                        maxWidth.value,
                        maxHeight.value,
                        video.format.ratio,
                        chromeDp = WIDE_CHROME_DP,
                        belowPanelDp = BELOW_PANEL_DP,
                    )
                WidePanes(
                    layout,
                    placement = placement,
                    belowHeight =
                        belowPanelHeightDp(
                                maxWidth.value,
                                maxHeight.value,
                                video.format.ratio,
                                WIDE_CHROME_DP,
                                BELOW_PANEL_DP,
                            )
                            .dp,
                    panelWidth = sidePanelWidthDp(maxWidth.value).dp,
                    // The panel meets the picture at a hairline, as on the desk, rather than across a gap.
                    gap = false,
                    animateOn = video.format,
                    main = {
                        Column(Modifier.fillMaxSize()) {
                            WideTopBar(ui = ui, onBack = leave, player = player)
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                // The picture as large as the column's width allows, the timeline right under it, and
                                // what height is left over above and below the two rather than between them.
                                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                    Picture(
                                        ui = ui,
                                        video = video,
                                        player = player,
                                        actions = actions,
                                        picture = picture,
                                        selected = graphSelected,
                                        onSelected = { graphSelected = it },
                                        reframing = reframing,
                                        formatStrip = false,
                                        toolbar = false,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    TimeBand(video, ui, player, zoom, onZoom = { zoom = it }, actions)
                                }
                                MessageBanner(ui.message, actions::clearMessage, Modifier.align(Alignment.TopCenter))
                            }
                        }
                    },
                    panel = {
                        // The graph's settings while the graph is selected, else the video's: its shot, its frame and
                        // the export. The graph's page stays where it was while something else (an export) borrows the
                        // sheet.
                        // Selecting the graph swaps the video's panel for the graph's: a cut, the selection already
                        // shows what changed.
                        AnimatedContent(
                            graphPanel,
                            modifier = Modifier.fillMaxSize().startRule(),
                            transitionSpec = { cut() },
                            label = "panel",
                        ) { graph ->
                            if (graph) {
                                val page = ui.sheet?.takeIf { it in PanelPages } ?: lastPage
                                LaunchedEffect(page) { lastPage = page }
                                EditPanel(
                                    page,
                                    title = stringResource(Res.string.edit_graph),
                                    onPage = actions::openSheet,
                                    onDone = { graphSelected = false },
                                    tools = {
                                        Pill(
                                            "${(video.overlay.opacity * 100).roundToInt()}%",
                                            icon = CgIcons.Contrast,
                                            onClick = { actions.overlay { it.withOpacity(nextOpacity(it.opacity)) } },
                                        )
                                    },
                                    footer = exportBar,
                                ) {
                                    AnimatedContent(
                                        page,
                                        transitionSpec = { cut() },
                                        label = "page",
                                    ) { shown ->
                                        Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S12)) {
                                            sheetBody(shown, video)
                                        }
                                    }
                                }
                            } else {
                                VideoPanel(
                                    ui = ui,
                                    video = video,
                                    actions = actions,
                                    reframing = reframing,
                                    onReframing = setReframing,
                                    onPickShotFile = onPickShotFile,
                                    footer = exportBar,
                                )
                            }
                        }
                    },
                )
            }
            // The export fades in over the editor, and out once its result is seen.
            ExportOver(overlay, ui, video, actions, onExported, fullScreen = false)
        }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding().then(keys)) {
        val sheetMax = maxHeight * 0.72f
        Column(Modifier.fillMaxSize()) {
            TopBar(ui, leave, actions, player)
            if (video == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(Res.string.edit_loading), style = CgTheme.type.body, color = CgColors.Text.Dim)
                }
                return@Column
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Picture(
                    ui = ui,
                    video = video,
                    player = player,
                    actions = actions,
                    picture = picture,
                    selected = graphSelected,
                    onSelected = { graphSelected = it },
                    reframing = reframing,
                    formatStrip = false,
                    onReframing = setReframing,
                    doneOnPicture = false,
                    modifier = Modifier.fillMaxSize(),
                )
                MessageBanner(ui.message, actions::clearMessage, Modifier.align(Alignment.TopCenter))
            }
            if (reframing) {
                // Framing: the formats under the picture, and "done" at the bottom right, where the timeline was.
                FormatPicker(
                    FORMAT_OPTIONS,
                    VideoFormat.entries.indexOf(video.format),
                    onPick = { actions.setFormat(VideoFormat.entries[it]) },
                )
                Box(Modifier.fillMaxWidth().padding(CgSpace.S16), contentAlignment = Alignment.CenterEnd) {
                    CgButton(
                        stringResource(Res.string.edit_done),
                        onClick = { setReframing(false) },
                        modifier = Modifier.widthIn(min = 112.dp).testTag(TestTags.EditFrame),
                    )
                }
            } else {
                // With the graph picked, only the filmstrip stays under the picture, so the picture gets the room.
                TimeBand(video, ui, player, zoom, onZoom = { zoom = it }, actions, tools = true)
            }
        }

        if (video != null) {
            // The export's own screen stands in for its sheet while it runs and until it is seen to.
            EditorSheetSlot(
                ui.sheet.takeUnless { phoneOverlay },
                Modifier.align(Alignment.BottomCenter),
                scrim = true,
                onScrimTap = actions::closeSheet.takeIf { ui.exporting == null },
            ) { sheet ->
                EditorSheet(
                    title = sheetTitle(sheet, ui),
                    onDone = actions::closeSheet.takeIf { ui.exporting == null },
                    modifier = Modifier.heightIn(max = sheetMax),
                ) {
                    sheetBody(sheet, video)
                }
            }
            ExportOver(phoneOverlay, ui, video, actions, onExported, fullScreen = true)
        }
    }
}

/**
 * The first export's offer to say outside the app when it is done; Allow asks the system, whatever it answers the offer
 * goes for good.
 */
@Composable
private fun exportAsk(ui: EditUiState, wording: ExportWording, actions: EditActions): ExportAsk? {
    val text = wording.notifyText ?: return null
    val ask = rememberNotificationPermission { actions.answerExportNotifications(it) }
    if (!ui.askExportNotifications || ui.exporting == null) return null
    return ExportAsk(
        title = stringResource(Res.string.edit_export_notify_title),
        text = stringResource(text),
        allow = stringResource(Res.string.edit_export_notify_allow),
        later = stringResource(Res.string.edit_export_notify_later),
        onAllow = ask,
        onLater = { actions.answerExportNotifications(false) },
    )
}

/**
 * The export over the editor while it runs and until its result is seen: a card over the dimmed window on a wide one,
 * the whole screen on a phone ([fullScreen]). Keep editing puts it down, and the export sheet with it.
 */
@Composable
private fun ExportOver(
    visible: Boolean,
    ui: EditUiState,
    video: VideoItem,
    actions: EditActions,
    onExported: () -> Unit,
    fullScreen: Boolean,
) {
    AnimatedVisibility(visible, enter = fadeIn(CgMotion.medium()), exit = fadeOut(CgMotion.medium())) {
        val wording = LocalExportWording.current
        val detail =
            listOfNotNull(
                    (ui.exportingQuality ?: ui.galleryQuality).size(video.format).let { "${it.width} × ${it.height}" },
                    clock(video.trimmedDurationUs),
                    video.shot?.profileName,
                )
                .joinToString(" · ")
        val doneNow =
            when {
                ui.exporting != null -> null
                ui.savedToGallery -> stringResource(wording.saved)
                else -> wording.shared?.let { stringResource(it) } ?: ""
            }
        // While it fades out the export is over and acknowledged: it keeps saying what it said.
        var done by remember { mutableStateOf(doneNow) }
        if (visible) done = doneNow
        ExportOverlay(
            detail = detail,
            footer = stringResource(wording.footer),
            progress = ui.exporting,
            left = ui.exportLeftMs?.let { clock(it * 1000) },
            done = done,
            still = ui.thumbnails.getOrNull(ui.thumbnails.size / 2),
            stillRatio = video.format.ratio,
            onCancel = actions::cancelExport,
            onKeepEditing = {
                actions.acknowledgeExport()
                actions.closeSheet()
            },
            next = stringResource(wording.next),
            onNext = {
                actions.acknowledgeExport()
                onExported()
            },
            fullScreen = fullScreen,
            leave = wording.leave?.let { stringResource(it) },
            ask = exportAsk(ui, wording, actions),
            savedKeys =
                ui.savedVideo?.let {
                    wording.openSaved?.let { open ->
                        ExportSavedKeys(open = stringResource(open), onOpen = actions::openSavedVideo)
                    }
                },
        )
    }
}

/**
 * Back, the shot with its status dot filling the rest of the bar, the sound key and the orange export. The reframe
 * switch sits on the picture ([Picture]), so the bar keeps room for the shot's name.
 */
@Composable
private fun TopBar(ui: EditUiState, onBack: () -> Unit, actions: EditActions, player: PlayerState) {
    val video = ui.item
    Row(
        Modifier.fillMaxWidth().bottomRule().padding(CgSpace.S8),
        horizontalArrangement = Arrangement.spacedBy(CgSpace.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        TopChip(
            text = video?.shot?.profileName ?: stringResource(Res.string.edit_add_shot),
            onClick = { actions.openSheet(EditSheet.SHOT) },
            modifier = Modifier.weight(1f).testTag(TestTags.EditShotChip),
            dot =
                when {
                    video?.shot == null -> CgColors.Text.Dim
                    video.shotStartInVideoUs != null -> CgColors.Status.Live
                    else -> CgColors.Status.Highlight
                },
        )
        SoundToggle(ui.item, player)
        if (LocalNativeChrome.current == null) {
            TopChip(
                stringResource(Res.string.edit_export),
                onClick = { actions.openSheet(EditSheet.EXPORT) },
                modifier = Modifier.testTag(TestTags.EditExport),
                primary = true,
            )
        }
    }
}

/** The picture filling the space, with the graph drawn over it and editable on it. */
@Composable
private fun Picture(
    ui: EditUiState,
    video: VideoItem,
    player: PlayerState,
    actions: EditActions,
    picture: @Composable (Modifier) -> Unit,
    selected: Boolean,
    onSelected: (Boolean) -> Unit,
    reframing: Boolean,
    /** The formats over the picture while framing: the phone's; a wide window has them in its panel. */
    formatStrip: Boolean = true,
    /** The toolbar over the selected graph: the phone's; a wide window has its tools in the panel. */
    toolbar: Boolean = true,
    /** The reframe switch over the picture: the phone's; a wide window has it in its panel. */
    onReframing: ((Boolean) -> Unit)? = null,
    /** The switch's "done" on the picture while framing; false where the screen puts it under the picture. */
    doneOnPicture: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // The video is Haze's source, so styles that blur what is behind the graph blur the video.
    val haze = rememberHazeState()
    val sheetOpen by rememberUpdatedState(ui.sheet != null)
    // Each tap that plays or pauses, for the mark that answers it.
    var taps by remember { mutableIntStateOf(0) }
    // While the graph is moved, the keys over the picture step aside, so it can go where they are.
    var graphMoving by remember { mutableStateOf(false) }
    val keys by animateFloatAsState(if (graphMoving) 0f else 1f, CgMotion.short(), label = "keys")
    BoxWithConstraints(
        modifier.fillMaxWidth().background(CgColors.Surface.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        // The video's own shape, as large as it fits: the width first (a wide format fills it), the height where the
        // width would make it too tall (a tall format fills the height). Another format picked grows or shrinks it
        // animated; the window turned or resized moves it at once.
        val (fitW, fitH) = fitRatio(maxWidth.value, maxHeight.value, video.format.ratio)
        val frame = animateSizeOn(DpSize(fitW.dp, fitH.dp), animateOn = video.format)
        // The frame is the same one the record screen filmed in, and the thumbnail it was picked from: it grows out of
        // either on the way in.
        Box(
            Modifier.shareBounds(SharedKeys.video(video.id), overlay = false)
                .shareBounds(SharedKeys.TAKE_FRAME, overlay = false)
                .size(frame.width, frame.height)
        ) {
            val reframe by rememberUpdatedState(video.reframe)
            // Tap to play; tap again to put down whatever is open. While reframing, drag and pinch move the picture
            // instead. The reframed picture is drawn inside the haze source, so a blur behind the graph blurs what the
            // export will show.
            Box(
                Modifier.fillMaxSize()
                    .clipToBounds()
                    .hazeSource(haze)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            when {
                                sheetOpen -> actions.closeSheet()
                                else -> {
                                    player.toggle()
                                    taps++
                                }
                            }
                        }
                    }
                    .then(
                        if (reframing)
                            Modifier.reframeGestures({ reframe }) { push, change -> actions.reframe(push, change) }
                        else Modifier
                    )
            ) {
                // The whole picture around the frame, moved and zoomed by the reframe, as the exporters draw it.
                Box(Modifier.fillMaxSize().reframed(video.reframe)) { picture(Modifier.coverOf(ui.sourceAspect)) }
            }
            // Framing: the frame's edge in the signal and its thirds, to line the cup up on.
            if (reframing) FrameGuides(Modifier.fillMaxSize())
            // The graph steps aside while the picture is framed, and comes back when framing is done.
            AnimatedVisibility(
                !reframing,
                Modifier.fillMaxSize(),
                enter = fadeIn(CgMotion.medium()),
                exit = fadeOut(CgMotion.short()),
            ) {
                video.shot?.let { shot ->
                    ShotOverlay(
                        shot = shot,
                        videoTimeUs = { player.positionUs },
                        shotStartUs = video.shotStartInVideoUs ?: 0L,
                        settings = video.overlay,
                        style = ui.style ?: return@let,
                        title = overlayTitleText(shot, video.overlay),
                        recipe = video.overlay.recipe,
                        fonts = CgTheme.overlayFonts,
                        modifier = Modifier.fillMaxSize(),
                        hazeState = haze,
                        backdropLuma = { ui.luma?.at(player.positionUs) },
                    )
                    OverlayEditLayer(
                        rect = video.overlay.rect,
                        format = video.format,
                        onChange = { rect, push -> actions.overlay(push) { it.copy(rect = rect) } },
                        onTap = { onSelected(!selected) },
                        selected = selected,
                        toolbar =
                            if (!toolbar) null
                            else {
                                {
                                    GraphToolbar(
                                        opacity = video.overlay.opacity,
                                        onStyle = { actions.openSheet(EditSheet.STYLE) },
                                        onData = { actions.openSheet(EditSheet.DATA) },
                                        onOpacity = { o -> actions.overlay { it.withOpacity(o) } },
                                    )
                                }
                            },
                        onDragging = { graphMoving = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            PlayMark(taps, player.playing, idle = !selected && !reframing, Modifier.align(Alignment.Center))
            var busy by remember { mutableStateOf(ui.busy) }
            if (ui.busy != null) busy = ui.busy
            AnimatedVisibility(
                ui.busy != null,
                Modifier.align(Alignment.TopStart).padding(CgSpace.S12).graphicsLayer { alpha = keys },
                enter = fadeIn(CgMotion.medium()),
                exit = fadeOut(CgMotion.medium()),
            ) {
                busy?.let { StatusChip(it.text(), recording = false) }
            }
            if (formatStrip) {
                AnimatedVisibility(
                    reframing,
                    Modifier.align(Alignment.TopCenter).padding(CgSpace.S12).clipToBounds(),
                    enter = slideInVertically(CgMotion.offsetSpec) { -it },
                    exit = slideOutVertically(CgMotion.offsetSpec) { -it },
                ) {
                    FormatStrip(video.format, onPick = actions::setFormat)
                }
            }
            AnimatedVisibility(
                reframing,
                Modifier.align(
                        when {
                            !doneOnPicture -> Alignment.TopStart
                            onReframing != null -> Alignment.BottomStart
                            else -> Alignment.BottomCenter
                        }
                    )
                    .padding(CgSpace.S8)
                    .clipToBounds(),
                enter = slideInVertically(CgMotion.offsetSpec) { if (doneOnPicture) it else -it },
                exit = slideOutVertically(CgMotion.offsetSpec) { if (doneOnPicture) it else -it },
            ) {
                StatusChip(stringResource(Res.string.edit_reframe_hint), recording = false)
            }
            // Top right while the graph is being edited, so it never covers the toolbar; bottom right beside the hint
            // while framing, clear of the formats along the top.
            onReframing
                ?.takeIf { doneOnPicture || !reframing }
                ?.let { set ->
                    // It travels between the two rather than jumping.
                    // It travels between the two rather than jumping; the slide is read in layout, so only the key
                    // moves, not the picture recomposing under it.
                    val bias by animateFloatAsState(if (reframing) 1f else -1f, CgMotion.settle(), label = "switch")
                    Pill(
                        if (reframing) stringResource(Res.string.edit_done)
                        else stringResource(Res.string.edit_frame_switch),
                        onClick = { set(!reframing) },
                        light = reframing,
                        icon = if (reframing) null else CgIcons.Frame,
                        overVideo = true,
                        modifier =
                            Modifier.align(Alignment.TopEnd)
                                .biasedDown { bias }
                                .padding(horizontal = CgSpace.S8)
                                .graphicsLayer { alpha = keys }
                                .testTag(TestTags.EditFrame),
                    )
                }
        }
    }
}

/**
 * The timeline and its tools: place the shot's start, find it by ear, step a frame, zoom. Without [tools], only the
 * filmstrip (the phone while the graph is picked).
 */
@Composable
private fun TimeBand(
    video: VideoItem,
    ui: EditUiState,
    player: PlayerState,
    zoom: Float?,
    onZoom: (Float?) -> Unit,
    actions: EditActions,
    tools: Boolean = true,
) {
    Column(Modifier.fillMaxWidth().topRule().background(CgColors.Surface.Base).padding(bottom = CgSpace.S8)) {
        EditTimeline(
            item = video,
            playheadUs = { player.positionUs },
            zoomFactor = zoom,
            thumbnails = ui.thumbnails,
            loading = ui.stripLoading,
            waveform = ui.waveform,
            // The pump in the sound only helps line a shot up by hand: a recording the machine or scale started is
            // already in step, and a line its flag does not sit on would only confuse.
            pumpUs = ui.pumpUs.takeIf { !video.syncLocked },
            actions =
                TimelineActions(
                    seek = {
                        player.playing = false
                        player.seek(it)
                    },
                    trim = { a, b, push -> actions.trim(a, b, push) },
                    shotStart = { us, push -> actions.setShotStart(us, push) },
                    zoom = onZoom,
                ),
            compact = !tools,
        )
        if (!tools) return@Column
        // A narrow pane (a book foldable's page) has no room for the gap between the two groups: the row scrolls
        // instead.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow = maxWidth < NARROW_TOOLS
            Row(
                Modifier.fillMaxWidth()
                    .then(if (narrow) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = CgSpace.S12),
                horizontalArrangement = Arrangement.spacedBy(CgSpace.S6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Lining the shot up by hand; a recording the machine or scale started is in step already, until
                // its start is unlocked.
                if (video.syncLocked && video.shot != null) {
                    Pill(
                        stringResource(Res.string.edit_graph_start),
                        onClick = actions::unlockShotStart,
                        icon = CgIcons.Lock,
                    )
                } else if (!video.syncLocked) {
                    val movable = video.shot != null
                    Pill(
                        stringResource(Res.string.edit_start_here),
                        onClick = { actions.setShotStart(player.positionUs, push = true) },
                        enabled = movable,
                        icon = CgIcons.Flag,
                    )
                    Pill(stringResource(Res.string.edit_find_start), onClick = actions::findStart, enabled = movable)
                }
                if (!narrow) Spacer(Modifier.weight(1f))
                Pill(
                    "",
                    onClick = {
                        player.playing = false
                        player.seek(TimelineMath.stepFrame(video, player.positionUs, -1))
                    },
                    icon = CgIcons.ChevronLeft,
                    iconDescription = stringResource(Res.string.edit_step_back),
                )
                Pill(
                    "",
                    onClick = {
                        player.playing = false
                        player.seek(TimelineMath.stepFrame(video, player.positionUs, 1))
                    },
                    icon = CgIcons.ChevronRight,
                    iconDescription = stringResource(Res.string.edit_step_forward),
                )
                Pill(
                    if (zoom == null) "" else stringResource(Res.string.edit_fit),
                    onClick = { onZoom(if (zoom == null) 4f else null) },
                    icon = if (zoom == null) CgIcons.Plus else null,
                    iconDescription = stringResource(Res.string.edit_zoom_in),
                )
            }
        }
    }
}

/** Narrower than this, the timeline's tools scroll rather than spread. */
private val NARROW_TOOLS = 400.dp

private fun Modifier.editorKeys(
    actions: EditActions,
    onExport: () -> Unit,
    blocked: () -> Boolean,
    onEscape: () -> Unit,
): Modifier = onPreviewKeyEvent { e ->
    if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) onEscape()
    // Under the export's overlay the editor takes no keys at all.
    if (blocked()) return@onPreviewKeyEvent true
    val command = e.type == KeyEventType.KeyDown && (e.isMetaPressed || e.isCtrlPressed)
    when {
        command && e.key == Key.Z -> if (e.isShiftPressed) actions.redo() else actions.undo()
        command && e.key == Key.E -> onExport()
        else -> return@onPreviewKeyEvent false
    }
    true
}

/** The wide panel's pages while the graph is selected: its settings. The shot and export are the video panel's. */
/** What always takes height on a wide window besides the picture: the top bar and the timeline band. */
private const val WIDE_CHROME_DP = 236f

/** The least the panel under the picture gets on an upright window: its pages scroll, the export stays pinned. */
private const val BELOW_PANEL_DP = 360f

private val PanelPages = listOf(EditSheet.STYLE, EditSheet.DATA)

/**
 * The wide editor's top bar: back, what is being edited (the shot, else the file), and the sound. The shot, the frame
 * and the export are in the side panel; undo and redo are Cmd/Ctrl+Z and Shift+Cmd/Ctrl+Z.
 */
@Composable
private fun WideTopBar(ui: EditUiState, onBack: () -> Unit, player: PlayerState) {
    val video = ui.item
    Row(
        Modifier.fillMaxWidth().bottomRule().padding(CgSpace.S8),
        horizontalArrangement = Arrangement.spacedBy(CgSpace.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        Text(
            (video?.shot?.profileName ?: video?.file.orEmpty()).caps(),
            style = CgTheme.type.label,
            color = CgColors.Text.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        SoundToggle(video, player)
    }
}

/**
 * Where the bars are native (iPhone Duo): export (prominent), undo and redo together, and the sound last, so it is the
 * first to go to the overflow. The keys they replace on the screen step aside. [onExport] is the wide editor's save;
 * null opens the phone's export sheet.
 */
@Composable
private fun EditNativeBar(ui: EditUiState, actions: EditActions, player: PlayerState, onExport: (() -> Unit)?) {
    val video = ui.item
    val export = stringResource(Res.string.edit_export)
    val undo = stringResource(Res.string.edit_undo)
    val redo = stringResource(Res.string.edit_redo)
    val sound = stringResource(if (player.muted) Res.string.edit_muted else Res.string.edit_sound)
    NativeItems(
        buildList {
            if (video != null) {
                add(
                    NativeBarItem(
                        TestTags.EditExport,
                        export,
                        BarIcon.Export,
                        prominent = true,
                        enabled = ui.exporting == null,
                        priority = BarPriority.High,
                    )
                )
            }
            add(NativeBarItem(TestTags.EditUndo, undo, BarIcon.Undo, enabled = ui.canUndo, group = 1))
            add(NativeBarItem(TestTags.EditRedo, redo, BarIcon.Redo, enabled = ui.canRedo, group = 1))
            if (video?.hasAudio == true) {
                val icon = if (player.muted) BarIcon.SoundOff else BarIcon.SoundOn
                add(NativeBarItem(TestTags.EditSound, sound, icon, bottom = true, priority = BarPriority.Low))
            }
        }
    ) { id ->
        when (id) {
            TestTags.EditExport -> onExport?.invoke() ?: actions.openSheet(EditSheet.EXPORT)
            TestTags.EditUndo -> actions.undo()
            TestTags.EditRedo -> actions.redo()
            TestTags.EditSound -> player.muted = !player.muted
        }
    }
}

/**
 * Sound on or off, for playback and the export alike; a video recorded without the microphone has nothing to mute, so
 * there is no key. Where the bars are native it is their item instead.
 */
@Composable
private fun SoundToggle(video: VideoItem?, player: PlayerState) {
    if (video?.hasAudio != true || LocalNativeChrome.current != null) return
    // A top-bar key like the shot and export ones beside it, the same height.
    TopChip(
        if (player.muted) stringResource(Res.string.edit_muted) else stringResource(Res.string.edit_sound),
        onClick = { player.muted = !player.muted },
        modifier = Modifier.testTag(TestTags.EditSound),
    )
}

/**
 * The editor's warning, over the top of the picture rather than above it, so the picture never changes size. It slides
 * down from under the bar and back up, still saying what it said while it goes.
 */
@Composable
private fun MessageBanner(message: UiText?, onOk: () -> Unit, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf(message) }
    if (message != null) last = message
    AnimatedVisibility(
        message != null,
        modifier.clipToBounds(),
        enter = slideInVertically(CgMotion.offsetSpec) { -it },
        exit = slideOutVertically(CgMotion.offsetSpec) { -it },
    ) {
        last?.let {
            Banner(it.text(), BannerKind.Warning, action = stringResource(Res.string.edit_ok), onAction = onOk)
        }
    }
}

/**
 * The wide window's side panel while nothing is selected: the video's settings. Its shot (one line and "change", which
 * opens the file and the machine's shots in place), its frame (the format, and zooming and cropping the picture in it),
 * the graph's style (changed by tapping the graph), and the [footer] export pinned at the bottom.
 */
@Composable
private fun VideoPanel(
    ui: EditUiState,
    video: VideoItem,
    actions: EditActions,
    reframing: Boolean,
    onReframing: (Boolean) -> Unit,
    onPickShotFile: () -> Unit,
    footer: @Composable () -> Unit,
) {
    // The machine's shots, for the list under the file.
    LaunchedEffect(ui.hasShotHistory) { if (ui.hasShotHistory) actions.loadShots() }
    var changingShot by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).animateContentSize(CgMotion.sizeSpec)) {
            val shot = video.shot
            // Picking another shot: the section is marked with the signal along its edge while it is open.
            PanelSection(marked = shot == null || changingShot) {
                PanelLabel(stringResource(Res.string.edit_shot))
                if (shot != null && !changingShot) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CgSpace.S12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(width = 4.dp, height = 36.dp).background(machineColor(shot.source)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "${shot.profileName} · ${machineLabel(shot.source)}".caps(),
                                style = CgTheme.type.label,
                            )
                            Text(shotDetail(video), style = CgTheme.type.data, color = CgColors.Text.Secondary)
                        }
                        Pill(stringResource(Res.string.edit_change), onClick = { changingShot = true })
                    }
                    if (video.syncLocked) {
                        Pill(
                            stringResource(Res.string.edit_graph_start),
                            onClick = actions::unlockShotStart,
                            icon = CgIcons.Lock,
                        )
                    } else if (video.shotStartByClock() != null) {
                        Pill(stringResource(Res.string.edit_match_by_clock), onClick = actions::matchByClock)
                    }
                } else {
                    ShotSheetContent(
                        video,
                        ui.shots,
                        onPick = {
                            actions.pickShot(it)
                            changingShot = false
                        },
                        onFile = onPickShotFile,
                        onMatchByClock = actions::matchByClock,
                        onUnlock = actions::unlockShotStart,
                        onFromMachine = LocalFromMachine.current,
                    )
                    if (shot != null) Pill(stringResource(Res.string.edit_done), onClick = { changingShot = false })
                }
            }
            PanelSection {
                PanelLabel(stringResource(Res.string.edit_frame))
                FormatPicker(
                    FORMAT_OPTIONS,
                    VideoFormat.entries.indexOf(video.format),
                    onPick = { actions.setFormat(VideoFormat.entries[it]) },
                    compact = true,
                )
                Pill(
                    if (reframing) stringResource(Res.string.edit_done)
                    else stringResource(Res.string.edit_zoom_and_crop),
                    onClick = { onReframing(!reframing) },
                    light = reframing,
                    modifier = Modifier.fillMaxWidth(),
                    fill = true,
                )
            }
            if (shot != null) {
                PanelSection(gap = CgSpace.S4) {
                    PanelLabel(stringResource(Res.string.edit_graph))
                    Text((ui.style?.name ?: video.overlay.styleId).caps(), style = CgTheme.type.label)
                    Text(
                        stringResource(Res.string.edit_tap_graph_hint),
                        style = CgTheme.type.caption,
                        color = CgColors.Text.Secondary,
                    )
                }
            }
        }
        PanelFooter(footer)
    }
}

/**
 * One part of the desk panel (the shot, the frame, the graph): padded, over a hairline. [marked] draws the signal along
 * its leading edge, for the part that is open for a change.
 */
@Composable
private fun PanelSection(
    marked: Boolean = false,
    gap: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .bottomRule()
            .then(if (marked) Modifier.leadingSignal() else Modifier)
            .padding(CgSpace.S16),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** The export pinned at the bottom of the desk panel, under a hairline. */
@Composable
private fun PanelFooter(footer: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().topRule().padding(horizontal = CgSpace.S16, vertical = CgSpace.S12)) { footer() }
}

/** "Gaggiuino · 0:40.0 · 3 phases · lined up by the clock": the shot under its name. */
@Composable
private fun shotDetail(video: VideoItem): String {
    val shot = video.shot ?: return ""
    val phases = shot.phaseStartsUs.size
    return clock(shot.durationUs) +
        (if (phases > 0) " · " + stringResource(Res.string.edit_phases_count, "$phases") else "") +
        " · " +
        stringResource(if (video.syncLocked) Res.string.edit_recorded_with_video else shotStatus(video))
}

@Composable
private fun PanelLabel(text: String) {
    Text(text.caps(), style = CgTheme.type.labelStrong, color = CgColors.Text.Secondary)
}

/**
 * The upper half on its side: back, what is edited, the sound on the left; the picture in its shape in the middle;
 * framing, the time and play on the right.
 */
@Composable
private fun PlayerHalf(
    ui: EditUiState,
    video: VideoItem,
    player: PlayerState,
    actions: EditActions,
    onBack: () -> Unit,
    reframing: Boolean,
    onReframing: (Boolean) -> Unit,
    picture: @Composable (Modifier) -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(CgSpace.S12), horizontalArrangement = Arrangement.spacedBy(CgSpace.S12)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(CgSpace.S8)) {
            BackButton(onClick = onBack)
            val shot = video.shot
            Text((shot?.profileName ?: video.file).caps(), style = CgTheme.type.screenTitle, maxLines = 2)
            if (shot != null) {
                Row(
                    Modifier.fillMaxWidth()
                        .border(CgStroke.Hairline, CgColors.Line.Hairline)
                        .padding(horizontal = CgSpace.S12, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(CgSpace.S12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(width = 4.dp, height = 32.dp).background(machineColor(shot.source)))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${shot.profileName} · ${machineLabel(shot.source)}".caps(), style = CgTheme.type.label)
                        Text(
                            shotDetail(video).removePrefix(" · "),
                            style = CgTheme.type.data,
                            color = CgColors.Text.Secondary,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            SoundToggle(video, player)
        }
        picture(Modifier.fillMaxHeight().aspectRatio(video.format.ratio, matchHeightConstraintsFirst = true))
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(CgSpace.S8),
            horizontalAlignment = Alignment.End,
        ) {
            Pill(
                if (reframing) stringResource(Res.string.edit_done) else stringResource(Res.string.edit_frame),
                onClick = { onReframing(!reframing) },
                light = reframing,
            )
            Spacer(Modifier.weight(1f))
            PlayheadClock(player)
            Text(
                stringResource(Res.string.edit_of_duration, clock(video.durationUs)),
                style = CgTheme.type.caption,
                color = CgColors.Text.Secondary,
            )
            val playLabel =
                if (player.playing) stringResource(Res.string.edit_pause) else stringResource(Res.string.edit_play)
            Box(
                Modifier.size(64.dp)
                    .border(CgStroke.Hairline, CgColors.Brand.Bone)
                    .clickable(role = Role.Button, onClick = player::toggle)
                    .semantics { contentDescription = playLabel },
                contentAlignment = Alignment.Center,
            ) {
                CgIcon(if (player.playing) CgIcons.Pause else CgIcons.Play, size = 28.dp)
            }
        }
    }
}

/** The tools under the timeline on its side, one chip each: the graph's style, data, text and opacity, and export. */
@Composable
private fun ToolChips(ui: EditUiState, video: VideoItem, actions: EditActions, onExport: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = CgSpace.S24)
            .topRule()
            .horizontalScroll(rememberScrollState())
            .padding(top = CgSpace.S12, bottom = CgSpace.S24),
        horizontalArrangement = Arrangement.spacedBy(CgSpace.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (video.shot != null) {
            Pill(
                stringResource(Res.string.edit_style_named, ui.style?.name ?: video.overlay.styleId),
                onClick = { actions.openSheet(EditSheet.STYLE) },
            )
            Pill(stringResource(Res.string.edit_data), onClick = { actions.openSheet(EditSheet.DATA) })
            Pill(
                stringResource(Res.string.edit_opacity, "${(video.overlay.opacity * 100).roundToInt()}"),
                onClick = { actions.overlay { it.withOpacity(nextOpacity(it.opacity)) } },
            )
        } else {
            Pill(stringResource(Res.string.edit_add_shot), onClick = { actions.openSheet(EditSheet.SHOT) })
        }
        if (LocalNativeChrome.current == null) {
            TopChip(stringResource(LocalExportWording.current.save), onClick = onExport, primary = true)
        }
    }
}

private val FORMAT_OPTIONS = VideoFormat.entries.map { FormatOption(it.label, it.ratio) }

/**
 * The video's shape, while framing: the four formats as one strip on an ink ground over the picture, like the camera's
 * lenses, the current one filled bone.
 */
@Composable
private fun FormatStrip(format: VideoFormat, onPick: (VideoFormat) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(CgColors.Surface.Base.copy(alpha = OVER_VIDEO))
            .border(CgStroke.Hairline, CgColors.Line.Hairline)
            .padding(CgStroke.Hairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (f in VideoFormat.entries) {
            val on = f == format
            Box(
                Modifier.testTag(TestTags.format(f.label))
                    .heightIn(min = CgSize.Touch)
                    .widthIn(min = 64.dp)
                    .background(if (on) CgColors.Brand.Bone else Color.Transparent)
                    .selectable(on, role = Role.RadioButton, onClick = { onPick(f) })
                    .padding(horizontal = CgSpace.S12),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    f.label,
                    style = CgTheme.type.labelStrong,
                    color = if (on) CgColors.Text.OnLight else CgColors.Text.Primary,
                )
            }
        }
    }
}

/** The frame being framed: a signal hairline round it and bone hairlines on its thirds. */
@Composable
private fun FrameGuides(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = CgStroke.Hairline.toPx()
        val thirds = CgColors.Text.Primary.copy(alpha = 0.25f)
        for (i in 1..2) {
            drawRect(thirds, Offset(size.width * i / 3f - w / 2, 0f), Size(w, size.height))
            drawRect(thirds, Offset(0f, size.height * i / 3f - w / 2), Size(size.width, w))
        }
        drawRect(CgColors.Brand.Signal, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
    }
}

@Composable
private fun sheetTitle(sheet: EditSheet, ui: EditUiState): String =
    stringResource(
        when (sheet) {
            EditSheet.EXPORT ->
                when {
                    ui.exporting != null -> Res.string.edit_making_video
                    ui.savedToGallery -> Res.string.edit_saved
                    else -> Res.string.edit_export
                }
            else -> sheet.label
        }
    )

/** A sheet's name, as its title and the wide panel's tabs say it. */
private val EditSheet.label: StringResource
    get() =
        when (this) {
            EditSheet.STYLE -> Res.string.edit_style
            EditSheet.DATA -> Res.string.edit_data
            EditSheet.SHOT -> Res.string.edit_shot
            EditSheet.EXPORT -> Res.string.edit_export
        }

/**
 * The wide window's side panel while the graph is selected: the graph's pages the phone shows as sheets (style, data,
 * text), one at a time, picked at its top, with the [tools] the phone's toolbar over the graph holds; "done" puts the
 * graph down.
 */
@Composable
private fun EditPanel(
    page: EditSheet,
    title: String,
    onPage: (EditSheet) -> Unit,
    onDone: () -> Unit,
    /** The graph's quick tools beside "done": how much of its background shows, and a reset of its place and size. */
    tools: @Composable () -> Unit,
    /** Pinned under the pages: the export. */
    footer: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().topRule()) {
        Row(
            Modifier.fillMaxWidth()
                .bottomRule()
                .padding(start = CgSpace.S16, end = CgSpace.S12, top = CgSpace.S12, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CgSpace.S6),
        ) {
            Text(title.caps(), style = CgTheme.type.sheetTitle, modifier = Modifier.weight(1f))
            tools()
            Pill(
                stringResource(Res.string.edit_done),
                onClick = onDone,
                light = true,
                modifier = Modifier.testTag(TestTags.SheetDone),
            )
        }
        SegmentedControl(
            PanelPages.map { stringResource(it.label) },
            PanelPages.indexOf(page),
            onSelect = { onPage(PanelPages[it]) },
            fill = true,
            modifier = Modifier.fillMaxWidth(),
            tags = listOf(TestTags.ToolbarStyle, TestTags.ToolbarData),
        )
        Column(
            Modifier.fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CgSpace.S16, vertical = CgSpace.S12),
            verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
        ) {
            content()
        }
        PanelFooter(footer)
    }
}

/**
 * The play or pause key in the middle of the picture, shown only when a tap on the picture plays or pauses it: it holds
 * a moment and then cuts out, so the tap is seen to. Never while something else is being worked on ([idle] false: the
 * graph picked, the picture framed).
 */
@Composable
private fun PlayMark(taps: Int, playing: Boolean, idle: Boolean, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(taps) {
        if (taps > 0 && idle) {
            shown.snapTo(1f)
            shown.animateTo(0f, tween(CgMotion.SHORT, delayMillis = PLAY_MARK_HOLD_MS, easing = CgMotion.Linear))
        } else shown.snapTo(0f)
    }
    Box(
        modifier
            .size(56.dp)
            .graphicsLayer { alpha = shown.value }
            .background(CgColors.Surface.Base.copy(alpha = 0.6f))
            .border(CgStroke.Hairline, CgColors.Brand.Bone),
        contentAlignment = Alignment.Center,
    ) {
        CgIcon(if (playing) CgIcons.Play else CgIcons.Pause, size = 24.dp)
    }
}

private const val PLAY_MARK_HOLD_MS = 300

/**
 * Where the playhead is, as a clock: its own scope, changing only when the shown time does (tenths), so playback does
 * not recompose the half of the screen around it every frame.
 */
@Composable
private fun PlayheadClock(player: PlayerState) {
    val shown by remember(player) { derivedStateOf { clock(player.positionUs) } }
    Text(shown, style = CgTheme.type.pivot)
}

/**
 * Places the content down its parent's height as `BiasAlignment(_, bias)` would (−1 top, 1 bottom), reading [bias]
 * while laying out: an animated bias then moves the content without recomposing anything.
 */
private fun Modifier.biasedDown(bias: () -> Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeable.height
    layout(placeable.width, height) {
        val space = (height - placeable.height).toFloat()
        placeable.place(0, (space / 2f * (1f + bias())).roundToInt())
    }
}

/** A hairline along the bottom edge: under the top bars. */
private fun Modifier.bottomRule(): Modifier = drawBehind {
    val h = CgStroke.Hairline.toPx()
    drawRect(CgColors.Line.Hairline, Offset(0f, size.height - h), Size(size.width, h))
}

/** A hairline along the leading edge: where the desk panel meets the picture. */
private fun Modifier.startRule(): Modifier = drawBehind {
    drawRect(CgColors.Line.Hairline, Offset.Zero, Size(CgStroke.Hairline.toPx(), size.height))
}

/** The signal along the leading edge: the desk panel's part that is open for a change. */
private fun Modifier.leadingSignal(): Modifier = drawBehind {
    drawRect(CgColors.Brand.Signal, Offset.Zero, Size(CgStroke.Rule.toPx(), size.height))
}

/** A hairline along the top edge: over the timeline's band. */
private fun Modifier.topRule(): Modifier = drawBehind {
    drawRect(CgColors.Line.Hairline, Offset.Zero, Size(size.width, CgStroke.Hairline.toPx()))
}
