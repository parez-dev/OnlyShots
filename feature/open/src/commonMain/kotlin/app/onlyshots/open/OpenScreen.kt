package app.onlyshots.open

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.onlyshots.TestTags
import app.onlyshots.core.designsystem.CgColors
import app.onlyshots.core.designsystem.CgIcons
import app.onlyshots.core.designsystem.CgMotion
import app.onlyshots.core.designsystem.CgSize
import app.onlyshots.core.designsystem.CgSpace
import app.onlyshots.core.designsystem.CgStroke
import app.onlyshots.core.designsystem.CgTheme
import app.onlyshots.core.designsystem.caps
import app.onlyshots.core.designsystem.component.BackButton
import app.onlyshots.core.designsystem.component.BarIcon
import app.onlyshots.core.designsystem.component.BarPriority
import app.onlyshots.core.designsystem.component.CgIcon
import app.onlyshots.core.designsystem.component.CgSheet
import app.onlyshots.core.designsystem.component.HeaderBack
import app.onlyshots.core.designsystem.component.NativeBarItem
import app.onlyshots.core.designsystem.component.NativeItems
import app.onlyshots.core.designsystem.component.OnlyShotsMark
import app.onlyshots.core.designsystem.component.SheetHeader
import app.onlyshots.core.designsystem.component.TwoPane
import app.onlyshots.core.designsystem.component.topRule
import app.onlyshots.core.model.Shot
import app.onlyshots.core.model.SidecarCodec
import app.onlyshots.core.model.VideoItem
import app.onlyshots.core.model.machineName
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.UiText
import app.onlyshots.core.resources.app_name
import app.onlyshots.core.resources.open_change
import app.onlyshots.core.resources.open_choose
import app.onlyshots.core.resources.open_dismiss
import app.onlyshots.core.resources.open_drop_to_use
import app.onlyshots.core.resources.open_editor
import app.onlyshots.core.resources.open_from_file
import app.onlyshots.core.resources.open_from_machine
import app.onlyshots.core.resources.open_from_machine_detail
import app.onlyshots.core.resources.open_header_edit
import app.onlyshots.core.resources.open_phone_shot_machine
import app.onlyshots.core.resources.open_phone_shot_title
import app.onlyshots.core.resources.open_phone_video_gallery
import app.onlyshots.core.resources.open_phone_video_title
import app.onlyshots.core.resources.open_sheet_title
import app.onlyshots.core.resources.open_shot_file_detail
import app.onlyshots.core.resources.open_shot_from
import app.onlyshots.core.resources.open_video
import app.onlyshots.core.resources.source_scale
import app.onlyshots.core.resources.text
import app.onlyshots.layout.LayoutKind
import app.onlyshots.layout.LocalWindowLayout
import app.onlyshots.layout.WindowLayout
import app.onlyshots.platform.rememberShotFilePicker
import app.onlyshots.platform.rememberVideoPicker
import app.onlyshots.platform.videoDrop
import app.onlyshots.testTagsAsResourceIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** The shot picked for the video: the shot itself, what to call it, and where it came from ("file" or a machine). */
data class ChosenShot(val shot: Shot, val label: String, val from: String)

/**
 * Every editor's start: pick the video and its shot, then open the editor, which lines the shot up. Both are needed:
 * the editor is for putting a shot's graph on a video. The shot comes from a `.shot` file, or from the machine itself
 * where the host can reach one ([onFromMachine]: the phones and the desktop, not the web), whose picker hands the shot
 * back through [pickedShot].
 *
 * The phones come here from their start screen ([onBack]); the desktop and the web start here and take a dropped video.
 */
@Composable
fun OpenScreen(
    onOpen: (id: String) -> Unit,
    onBack: (() -> Unit)? = null,
    onFromMachine: (() -> Unit)? = null,
    pickedShot: StateFlow<String?> = MutableStateFlow(null),
    onPickedShotUsed: () -> Unit = {},
    /** What the desktop or the web says about itself (where it runs, what its videos come out as). */
    host: OpenHost? = null,
    /** The phone app's store links, which the web offers. */
    stores: StoreLinks? = null,
    /** The app's own privacy screen, for the header's "privacy" (desktop, web). */
    onPrivacy: (() -> Unit)? = null,
    viewModel: OpenViewModel = koinViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val picked by viewModel.picked.collectAsStateWithLifecycle()
    val shot by viewModel.shot.collectAsStateWithLifecycle()
    val pickedFile by viewModel.pickedFile.collectAsStateWithLifecycle()
    val pickedThumb by viewModel.pickedThumb.collectAsStateWithLifecycle()
    val fromMachine by pickedShot.collectAsStateWithLifecycle()
    val scale = stringResource(Res.string.source_scale)
    LaunchedEffect(fromMachine) {
        val json = fromMachine ?: return@LaunchedEffect
        runCatching { SidecarCodec.json.decodeFromString(Shot.serializer(), json) }
            .getOrNull()
            ?.let { viewModel.chooseShot(it, it.profileName, machineName(it.source, scale)) }
        onPickedShotUsed()
    }
    val pickVideo = rememberVideoPicker(viewModel::pickVideo)
    val pickFile = rememberShotFilePicker(viewModel::chooseFile)
    var choosingSource by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val layout = LocalWindowLayout.current
    // Where the bars are native (iPhone Duo): open the editor (prominent) and, at the far end, a shot from a file.
    val openTitle = stringResource(Res.string.open_editor)
    val fileTitle = stringResource(Res.string.open_from_file)
    NativeItems(
        listOf(
            NativeBarItem(
                TestTags.OpenEditor,
                openTitle,
                BarIcon.Open,
                prominent = true,
                enabled = picked != null && shot != null,
                priority = BarPriority.High,
            ),
            NativeBarItem(TestTags.OpenFromFile, fileTitle, BarIcon.File, bottom = true, priority = BarPriority.Low),
        )
    ) { id ->
        if (id == TestTags.OpenEditor) viewModel.openWith(onOpen) else pickFile()
    }
    Box(Modifier.fillMaxSize()) {
        OpenBody(
            picked = picked,
            pickedFile = pickedFile?.copy(thumb = pickedThumb),
            shot = shot,
            status = status,
            onPickVideo = pickVideo,
            // With the file the only way (the web), the file picker opens at once rather than a sheet with one choice.
            onPickShot = if (onFromMachine == null) pickFile else ({ choosingSource = true }),
            fromMachine = onFromMachine != null,
            host = host,
            stores = stores,
            onPrivacy = onPrivacy,
            onOpenEditor = { viewModel.openWith(onOpen) },
            onBack = onBack,
            dragging = dragging,
            modifier = Modifier.videoDrop(onFile = viewModel::pickVideo, onHover = { dragging = it }),
        )
        if (choosingSource) {
            val choices: @Composable () -> Unit = {
                ShotSourceChoices(
                    onFile = {
                        choosingSource = false
                        pickFile()
                    },
                    onMachine =
                        onFromMachine?.let { open ->
                            {
                                choosingSource = false
                                open()
                            }
                        },
                )
            }
            val title = stringResource(Res.string.open_sheet_title)
            // On a fold the sheet is the right page, over the picked video, and the steps stay in view under a scrim.
            if (layout.fold && onBack != null) {
                FoldPageSheet(title, layout, onDismiss = { choosingSource = false }) { choices() }
            } else {
                CgSheet(title = title, onDismiss = { choosingSource = false }) { choices() }
            }
        }
    }
}

/**
 * The start from plain state: the two steps, and the button that opens the editor once both are picked. [dragging]
 * while a file is held over the window.
 */
@Composable
fun OpenBody(
    picked: VideoItem?,
    shot: ChosenShot?,
    status: UiText?,
    onPickVideo: () -> Unit,
    onPickShot: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    dragging: Boolean = false,
    /** The machine is one way to the shot (phones, desktop), not only a file (web). */
    fromMachine: Boolean = true,
    /** What the desktop or the web says about itself. */
    host: OpenHost? = null,
    stores: StoreLinks? = null,
    /** The picked video's own name and size, for the desktop's and the web's picked step. */
    pickedFile: PickedFile? = null,
    onPrivacy: (() -> Unit)? = null,
) {
    // The desktop and the web start here: the "OnlyShots Open Screen" design's page. The phones come from their start
    // screen to the column below.
    if (onBack == null) {
        WideOpenBody(
            picked = picked,
            pickedFile = pickedFile,
            shot = shot,
            status = status,
            onPickVideo = onPickVideo,
            onPickShot = onPickShot,
            onOpenEditor = onOpenEditor,
            modifier = modifier,
            dragging = dragging,
            fromMachine = fromMachine,
            host = host,
            stores = stores,
            onPrivacy = onPrivacy,
        )
        return
    }
    val layout = LocalWindowLayout.current
    if (layout.kind == LayoutKind.Wide) {
        WideCompose(
            layout = layout,
            onBack = onBack,
            picked = picked,
            pickedFile = pickedFile,
            shot = shot,
            status = status,
            dragging = dragging,
            fromMachine = fromMachine,
            onPickVideo = onPickVideo,
            onPickShot = onPickShot,
            onOpenEditor = onOpenEditor,
            modifier = modifier,
        )
        return
    }
    Page(onBack = onBack, modifier = modifier) {
        Column(
            Modifier.weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CgSpace.S16)
                .padding(top = CgSpace.S8),
            verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
        ) {
            Steps(picked, shot, status, dragging, fromMachine, onPickVideo, onPickShot)
            status?.takeIf { it != Opening }?.let { StatusLine(it.text()) }
            if (picked != null) PickedVideo(picked, pickedFile)
        }
        OpenEditorKey(
            ready = picked != null && shot != null,
            onClick = onOpenEditor,
            fill = true,
            modifier = Modifier.padding(CgSpace.S16),
        )
    }
}

/** The two steps in their frame, as every layout of the phone app's Open shows them. [large] on a big tablet. */
@Composable
private fun Steps(
    picked: VideoItem?,
    shot: ChosenShot?,
    status: UiText?,
    dragging: Boolean,
    fromMachine: Boolean,
    onPickVideo: () -> Unit,
    onPickShot: () -> Unit,
    large: Boolean = false,
) {
    val opening = status?.takeIf { it == Opening }?.text()
    StepFrame {
        StepRow(
            number = 1,
            title = stringResource(Res.string.open_phone_video_title),
            detail =
                opening
                    ?: picked?.let(::videoDetail)
                    ?: if (dragging) stringResource(Res.string.open_drop_to_use)
                    else stringResource(Res.string.open_phone_video_gallery),
            action = stringResource(if (picked == null) Res.string.open_choose else Res.string.open_change),
            done = picked != null,
            next = picked == null,
            highlighted = dragging,
            large = large,
            onClick = onPickVideo,
            modifier = Modifier.testTag(TestTags.OpenPickVideo),
        )
        StepDivider()
        StepRow(
            number = 2,
            title = stringResource(Res.string.open_phone_shot_title),
            detail =
                shot?.let {
                    if (it.from == "file") it.label else stringResource(Res.string.open_shot_from, it.label, it.from)
                }
                    ?: if (fromMachine) stringResource(Res.string.open_phone_shot_machine)
                    else stringResource(Res.string.open_shot_file_detail),
            action = stringResource(if (shot == null) Res.string.open_choose else Res.string.open_change),
            done = shot != null,
            next = picked != null && shot == null,
            large = large,
            onClick = onPickShot,
            modifier = Modifier.testTag(TestTags.OpenPickShot),
        )
    }
}

/**
 * The phone app's Open on a tablet or an open foldable (Open-Phone/Open-Ready -M, -B, -F):
 * - landscape: the steps on the left, the picked video in 9:16 and "open editor" in a 300 dp column on the right;
 * - portrait: the steps beside the picked video, "open editor" across the bottom;
 * - on a fold: the header and steps on the left page, the picked video and "open editor" on the right.
 */
@Composable
private fun WideCompose(
    layout: WindowLayout,
    onBack: () -> Unit,
    picked: VideoItem?,
    pickedFile: PickedFile?,
    shot: ChosenShot?,
    status: UiText?,
    dragging: Boolean,
    fromMachine: Boolean,
    onPickVideo: () -> Unit,
    onPickShot: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier,
) {
    val ready = picked != null && shot != null
    val problem = status?.takeIf { it != Opening }?.text()
    Box(modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding()) {
        if (layout.fold) {
            // [layout] measures from the window's edge, the pages from inside the safe area.
            val start = WindowInsets.safeDrawing.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
            TwoPane(
                vertical = false,
                splitDp = (layout.splitDp.dp - start).coerceAtLeast(0.dp),
                hingeDp = layout.hingeDp.dp,
                first = {
                    Column(
                        Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, top = CgSpace.S24, bottom = CgSpace.S24),
                        verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                    ) {
                        ComposeHeader(onBack, big = false)
                        Steps(picked, shot, status, dragging, fromMachine, onPickVideo, onPickShot)
                        problem?.let { StatusLine(it) }
                    }
                },
                second = {
                    Column(
                        Modifier.fillMaxSize()
                            .drawBehind {
                                drawRect(CgColors.Line.Divider, size = Size(CgStroke.Hairline.toPx(), size.height))
                            }
                            .padding(
                                start = CgSpace.S24,
                                end = CgSpace.S24,
                                top = FoldPreviewTop,
                                bottom = CgSpace.S24,
                            ),
                        verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PickedPreview(
                            picked,
                            pickedFile,
                            maxHeight = 384.dp,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.weight(1f))
                        OpenEditorKey(ready = ready, onClick = onOpenEditor, fill = true)
                    }
                },
            )
            return@Box
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth > maxHeight) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    ComposeHeader(onBack, big = true)
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                        ) {
                            Steps(picked, shot, status, dragging, fromMachine, onPickVideo, onPickShot, large = true)
                            problem?.let { StatusLine(it) }
                        }
                        Column(
                            Modifier.width(300.dp).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PickedPreview(
                                picked,
                                pickedFile,
                                maxHeight = 448.dp,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Spacer(Modifier.weight(1f))
                            OpenEditorKey(ready = ready, onClick = onOpenEditor, fill = true)
                        }
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = CgSpace.S24, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    ComposeHeader(onBack, big = true)
                    Row(
                        Modifier.weight(1f, fill = false).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                        ) {
                            Steps(picked, shot, status, dragging, fromMachine, onPickVideo, onPickShot)
                            problem?.let { StatusLine(it) }
                        }
                        PickedPreview(picked, pickedFile, maxHeight = 480.dp, modifier = Modifier.width(270.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    OpenEditorKey(ready = ready, onClick = onOpenEditor, fill = true)
                }
            }
        }
    }
}

/** The page's back key and "compose a video" over a hairline; Mono 28 on a tablet, 20 on a fold's page. */
@Composable
private fun ComposeHeader(onBack: () -> Unit, big: Boolean) {
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                drawRect(
                    CgColors.Line.Hairline,
                    Offset(0f, size.height - CgStroke.Hairline.toPx()),
                    Size(size.width, CgStroke.Hairline.toPx()),
                )
            }
            .padding(bottom = if (big) CgSpace.S16 else CgSpace.S12),
        horizontalArrangement = Arrangement.spacedBy(if (big) CgSpace.S16 else CgSpace.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onBack)
        Text(
            stringResource(Res.string.open_header_edit).caps(),
            style =
                if (big) CgTheme.type.pivot.copy(letterSpacing = 0.04.em, lineHeight = TextUnit.Unspecified)
                else CgTheme.type.screenTitle,
            modifier = Modifier.semantics { heading() },
        )
    }
}

/**
 * The picked video in 9:16, at most [maxHeight] tall and as wide as it is let, with its length on it and its name and
 * size under it; before one is picked, a dashed frame where it will be.
 */
@Composable
private fun PickedPreview(picked: VideoItem?, file: PickedFile?, maxHeight: Dp, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CgSpace.S12)) {
        Box(
            Modifier.weight(1f, fill = false)
                .heightIn(max = maxHeight)
                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                .then(
                    if (picked == null) Modifier.drawBehind { dashedBorder(CgColors.Line.Hairline) }
                    else Modifier.border(CgStroke.Hairline, CgColors.Line.Hairline)
                )
        ) {
            if (picked != null) {
                VideoTile(file?.thumb, modifier = Modifier.fillMaxSize())
                LengthTag(picked, Modifier.align(Alignment.BottomStart))
            }
        }
        if (picked != null) VideoFacts(file)
    }
}

/** The video's length on its frame, bottom left. */
@Composable
private fun LengthTag(picked: VideoItem, modifier: Modifier = Modifier) {
    Text(
        length(picked.trimmedDurationUs),
        style = CgTheme.type.labelStrong.copy(letterSpacing = 0.em),
        modifier =
            modifier
                .padding(6.dp)
                .background(CgColors.Surface.Base.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/** "VIDEO", the file's name and its size, beside or under the picked video's frame. */
@Composable
private fun VideoFacts(file: PickedFile?) {
    Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S4)) {
        Text(
            stringResource(Res.string.open_video).caps(),
            style = CgTheme.type.caption,
            color = CgColors.Text.Secondary,
        )
        Text(
            file?.name ?: stringResource(Res.string.open_video),
            style = CgTheme.type.label.copy(letterSpacing = 0.em),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        file
            ?.takeIf { it.widthPx > 0 && it.heightPx > 0 }
            ?.let {
                Text("${it.widthPx} × ${it.heightPx}", style = CgTheme.type.caption, color = CgColors.Text.Secondary)
            }
    }
}

/**
 * A sheet on a fold (Open-Source-F): the right page, under a signal rule, with its header and DONE key; the left page
 * dimmed under a scrim that closes it, as does back.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FoldPageSheet(
    title: String,
    layout: WindowLayout,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None)) { onDismiss() }
    TwoPane(
        vertical = false,
        splitDp = layout.splitDp.dp,
        hingeDp = layout.hingeDp.dp,
        modifier = Modifier.testTagsAsResourceIds(),
        first = {
            Box(
                Modifier.fillMaxSize()
                    .background(CgColors.Surface.Scrim.copy(alpha = 0.6f))
                    .clickable(
                        remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = stringResource(Res.string.open_dismiss),
                        onClick = onDismiss,
                    )
            )
        },
        second = {
            Column(
                Modifier.fillMaxSize()
                    .background(CgColors.Surface.Bar)
                    .topRule()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.End + WindowInsetsSides.Bottom
                        )
                    )
                    .padding(top = CgStroke.Rule + CgSpace.S24),
                verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
            ) {
                SheetHeader(title, onDone = onDismiss)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = CgSpace.S16),
                    verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
                    content = content,
                )
            }
        },
    )
}

/** On a fold, the picked video's top sits level with the steps on the other page (below their header). */
private val FoldPreviewTop = 105.dp

/** What the shot sheet offers: a file, and the machine where the host can reach one. */
@Composable
fun ShotSourceChoices(onFile: () -> Unit, onMachine: (() -> Unit)?) {
    Column {
        SourceChoice(
            title = stringResource(Res.string.open_from_file),
            detail = stringResource(Res.string.open_shot_file_detail),
            onClick = onFile,
            modifier = Modifier.testTag(TestTags.OpenFromFile),
        ) {
            CgIcon(CgIcons.File, size = 18.dp)
        }
        if (onMachine != null) {
            SourceChoice(
                title = stringResource(Res.string.open_from_machine),
                detail = stringResource(Res.string.open_from_machine_detail),
                onClick = onMachine,
                // The cells share their hairline, as the canvas stacks them.
                modifier = Modifier.offset(y = -CgStroke.Hairline).testTag(TestTags.OpenFromMachine),
            ) {
                // The machine's colour bar, as its rows wear it.
                Box(Modifier.size(width = CgSpace.S4, height = CgSpace.S24).background(CgColors.Machine.Gaggiuino))
            }
        }
    }
}

/**
 * One way to get the shot, in the shot sheet: a hairline cell with a 44 dp framed [tile], the way in Mono capitals,
 * what it takes, and a chevron.
 */
@Composable
private fun SourceChoice(
    title: String,
    detail: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tile: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .border(CgStroke.Hairline, CgColors.Line.Hairline)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(CgSpace.S16),
        horizontalArrangement = Arrangement.spacedBy(CgSpace.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(CgSize.Touch).border(CgStroke.Hairline, CgColors.Line.Hairline),
            contentAlignment = Alignment.Center,
        ) {
            tile()
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title.caps(), style = CgTheme.type.label)
            Text(detail, style = CgTheme.type.bodySmall, color = CgColors.Text.Secondary)
        }
        CgIcon(CgIcons.ChevronRight, tint = CgColors.Text.Secondary, size = 18.dp)
    }
}

/** A back header (phones) or the OnlyShots name (desktop, web) over a column, centred on a wide window. */
@Composable
private fun Page(onBack: (() -> Unit)?, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding(), Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
            if (onBack != null) {
                HeaderBack(
                    stringResource(Res.string.open_header_edit),
                    onBack = onBack,
                    modifier = Modifier.padding(vertical = CgSpace.S8),
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().height(72.dp).padding(horizontal = CgSpace.S16),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OnlyShotsMark(splashTarget = true)
                    Text(stringResource(Res.string.app_name).caps(), style = CgTheme.type.sheetTitle)
                }
            }
            content()
        }
    }
}

/** The steps' hairline frame: the two rows share it, with a hairline between them. */
@Composable
internal fun StepFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().border(CgStroke.Hairline, CgColors.Line.Hairline), content = content)
}

@Composable
internal fun StepDivider() {
    Box(Modifier.fillMaxWidth().height(CgStroke.Hairline).background(CgColors.Line.Hairline))
}

/**
 * A numbered step on the phones, from the canvas's Open-Phone and Open-Ready: its number in a 36 dp square (signal when
 * it is next, bone with a tick once done), what it is, what was picked (or what to pick), and the key. The next step
 * carries a signal rule down its leading edge; the whole row takes the tap.
 */
@Composable
internal fun StepRow(
    number: Int,
    title: String,
    detail: String,
    action: String,
    done: Boolean,
    next: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    large: Boolean = false,
) {
    // A step lights up as it is done or hovered with a file, rather than switching at once; read while drawing.
    val ground = animateColorAsState(if (highlighted) CgColors.Surface.Raised else Color.Transparent, CgMotion.short())
    val box = animateColorAsState(if (done) CgColors.Brand.Bone else Color.Transparent, CgMotion.medium())
    val edge =
        when {
            done -> CgColors.Brand.Bone
            highlighted -> CgColors.Brand.Signal
            else -> CgColors.Line.Hairline
        }
    val digit =
        when {
            done -> CgColors.Text.OnLight
            next -> CgColors.Text.Primary
            else -> CgColors.Text.Secondary
        }
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind { drawRect(ground.value) }
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                if (next || highlighted) {
                    drawRect(CgColors.Brand.Signal, size = Size(LeadRule.toPx(), size.height))
                }
            }
            .padding(if (large) CgSpace.S24 else CgSpace.S16),
        horizontalArrangement = Arrangement.spacedBy(if (large) CgSpace.S16 else CgSpace.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(if (large) CgSize.Touch else 36.dp)
                .drawBehind { drawRect(box.value) }
                .border(CgStroke.Hairline, edge),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                CgIcon(CgIcons.Check, tint = digit, size = 18.dp)
            } else {
                Text(stepNumber(number), style = CgTheme.type.labelStrong.copy(letterSpacing = 0.em), color = digit)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title.caps(),
                style = (if (large) CgTheme.type.screenTitle else CgTheme.type.label).copy(letterSpacing = 0.em),
            )
            // "choose a video" ticks over to the picked file's name and length.
            AnimatedContent(
                detail,
                transitionSpec = { with(CgMotion) { tick() } },
                label = "detail",
            ) {
                Text(
                    it,
                    style =
                        when {
                            done -> CgTheme.type.data
                            large -> CgTheme.type.body
                            else -> CgTheme.type.bodySmall
                        },
                    color = CgColors.Text.Secondary,
                )
            }
        }
        StepKey(action, signal = next && !done, onClick = null)
    }
}

/** The signal rule down the leading edge of the step that is next. */
private val LeadRule = 3.dp

/**
 * The picked video on the phones, from Open-Ready: a frame of it in 9:16 with its length on it, and beside it, its name
 * and size.
 */
@Composable
private fun PickedVideo(picked: VideoItem, file: PickedFile?) {
    Row(horizontalArrangement = Arrangement.spacedBy(CgSpace.S16), verticalAlignment = Alignment.Bottom) {
        Box(Modifier.size(158.dp, 280.dp).border(CgStroke.Hairline, CgColors.Line.Hairline)) {
            VideoTile(file?.thumb, modifier = Modifier.fillMaxSize().padding(CgStroke.Hairline))
            LengthTag(picked, Modifier.align(Alignment.BottomStart))
        }
        Box(Modifier.padding(bottom = CgSpace.S4)) { VideoFacts(file) }
    }
}

/** "2026-10-19 08:11 · 0:30": when the video was filmed and how long it is. */
private fun videoDetail(item: VideoItem): String {
    val seconds = item.trimmedDurationUs / 1_000_000
    val length = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    return item.createdAt.toString().take(16).replace('T', ' ') + " · " + length
}
