package app.onlyshots.style

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.onlyshots.SharedKeys
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
import app.onlyshots.core.designsystem.component.CgButton
import app.onlyshots.core.designsystem.component.CgButtonKind
import app.onlyshots.core.designsystem.component.CgDialog
import app.onlyshots.core.designsystem.component.CgIcon
import app.onlyshots.core.designsystem.component.CgSlider
import app.onlyshots.core.designsystem.component.CgTextField
import app.onlyshots.core.designsystem.component.CgToggle
import app.onlyshots.core.designsystem.component.CoffeeScene
import app.onlyshots.core.designsystem.component.DialogRisk
import app.onlyshots.core.designsystem.component.EditorSheet
import app.onlyshots.core.designsystem.component.EditorSheetSlot
import app.onlyshots.core.designsystem.component.LocalNativeChrome
import app.onlyshots.core.designsystem.component.NativeBarItem
import app.onlyshots.core.designsystem.component.NativeItems
import app.onlyshots.core.model.NormRect
import app.onlyshots.core.model.OverlaySettings
import app.onlyshots.core.model.SourceKind
import app.onlyshots.core.model.VideoItem
import app.onlyshots.core.model.sample.SampleShots
import app.onlyshots.core.model.style.GlassSettings
import app.onlyshots.core.model.style.GraphStyle
import app.onlyshots.core.model.style.Material
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.UiText
import app.onlyshots.core.resources.raw
import app.onlyshots.core.resources.style_cancel
import app.onlyshots.core.resources.style_discard
import app.onlyshots.core.resources.style_fix_it
import app.onlyshots.core.resources.style_glass_contrast
import app.onlyshots.core.resources.style_glass_corner
import app.onlyshots.core.resources.style_glass_curve
import app.onlyshots.core.resources.style_glass_dispersion
import app.onlyshots.core.resources.style_glass_edge
import app.onlyshots.core.resources.style_glass_frost
import app.onlyshots.core.resources.style_glass_refraction
import app.onlyshots.core.resources.style_glass_saturation
import app.onlyshots.core.resources.style_grid_colour
import app.onlyshots.core.resources.style_hex_colour
import app.onlyshots.core.resources.style_live_camera
import app.onlyshots.core.resources.style_name
import app.onlyshots.core.resources.style_opacity
import app.onlyshots.core.resources.style_panels
import app.onlyshots.core.resources.style_preview_drag
import app.onlyshots.core.resources.style_preview_record
import app.onlyshots.core.resources.style_readability_text_panel
import app.onlyshots.core.resources.style_readability_text_tint
import app.onlyshots.core.resources.style_readability_title
import app.onlyshots.core.resources.style_rename
import app.onlyshots.core.resources.style_save
import app.onlyshots.core.resources.style_save_ellipsis
import app.onlyshots.core.resources.style_save_style
import app.onlyshots.core.resources.style_save_this_style
import app.onlyshots.core.resources.style_scale_only
import app.onlyshots.core.resources.style_tab_grid
import app.onlyshots.core.resources.style_tab_lines
import app.onlyshots.core.resources.style_tab_panel
import app.onlyshots.core.resources.style_tab_readings
import app.onlyshots.core.resources.style_tab_text
import app.onlyshots.core.resources.style_unsaved_changes
import app.onlyshots.core.resources.style_use_by_default_for
import app.onlyshots.core.resources.style_your_video
import app.onlyshots.core.resources.text
import app.onlyshots.edit.PlayerState
import app.onlyshots.edit.VideoSurface
import app.onlyshots.edit.machineColor
import app.onlyshots.layout.LayoutKind
import app.onlyshots.layout.LocalWindowLayout
import app.onlyshots.layout.WidePanes
import app.onlyshots.overlay.compose.ShotOverlay
import app.onlyshots.overlay.compose.label
import app.onlyshots.overlay.fixReadability
import app.onlyshots.overlay.readabilityIssue
import app.onlyshots.overlay.relativeLuminance
import app.onlyshots.shareBounds
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The four parts of a style, one tab each. */
enum class StyleTab(val label: StringResource) {
    PANEL(Res.string.style_tab_panel),
    LINES(Res.string.style_tab_lines),
    GRID(Res.string.style_tab_grid),
    TEXT(Res.string.style_tab_text),
    READINGS(Res.string.style_tab_readings),
}

/** Which colour of the style the colour sheet is changing, and whether it carries its own opacity. */
class ColorTarget(
    val title: UiText,
    val get: (GraphStyle) -> Int,
    val set: (GraphStyle, Int) -> GraphStyle,
    val withOpacity: Boolean = false,
)

/**
 * The camera the record screen lends the style editor: its picture ([drawFrames] when a blur or glass style needs the
 * frames drawn by Compose) and the brightness of a rect of it, for adapt to video. A handle, so this module needs no
 * camera of its own.
 */
class LiveCamera(
    val sampleLuma: (x: Float, y: Float, w: Float, h: Float) -> Float?,
    val picture: @Composable (Modifier, drawFrames: Boolean) -> Unit,
)

/**
 * The picture under the preview while it is live: the camera (opened from the record screen) or the video playing
 * (opened from the editor), with how bright it is behind the graph, for adapt to video and the readability guard.
 */
class LivePicture(val caption: String, val luma: Float?, val content: @Composable (Modifier) -> Unit)

/**
 * Figma 13 · B–G: the graph style editor. A live preview: the camera when opened from the record screen, the video
 * playing when opened from the editor, else a frame of the user's newest video (drag to try other frames), tabs for
 * panel · lines · grid · text · readings, controls under them; every change shows at once. "save" names the style and
 * sets where it is the default; leaving with changes asks first.
 */
@Composable
fun StyleEditorScreen(
    baseId: String?,
    editId: String?,
    videoId: String?,
    onClose: (savedId: String?) -> Unit,
    /** Opened from the record screen: the live camera under the preview, lent by the host. */
    camera: LiveCamera? = null,
    viewModel: StyleEditorViewModel =
        koinViewModel(key = "style-$baseId-$editId-$videoId") {
            parametersOf(baseId.orEmpty(), editId.orEmpty(), videoId.orEmpty())
        },
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val frames by viewModel.frames.collectAsStateWithLifecycle()
    val video by viewModel.video.collectAsStateWithLifecycle()
    var saving by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }
    val live =
        when {
            camera != null -> liveCamera(draft, camera)
            else -> video?.let { liveVideo(it, draft, frames, viewModel) }
        }
    StyleEditorBody(
        draft = draft,
        frames = frames,
        live = live,
        sharedKey = (editId ?: baseId)?.let(SharedKeys::style),
        isNew = viewModel.isNew,
        onChange = viewModel::change,
        onBack = { if (viewModel.dirty) askDiscard = true else onClose(null) },
        onSave = { saving = true },
        sheetOverride =
            if (saving) {
                {
                    SaveSheet(
                        name = draft.name,
                        defaults = viewModel.defaults(),
                        onSave = { name, defaults -> onClose(viewModel.save(name, defaults)) },
                        onCancel = { saving = false },
                    )
                }
            } else null,
    )
    if (askDiscard) {
        CgDialog(
            title = stringResource(Res.string.style_save_this_style),
            text = stringResource(Res.string.style_unsaved_changes, draft.name),
            onDismiss = { askDiscard = false },
            confirm =
                stringResource(Res.string.style_save_ellipsis) to
                    {
                        askDiscard = false
                        saving = true
                    },
            dismiss = stringResource(Res.string.style_discard) to { onClose(null) },
            risk = DialogRisk.Dismiss,
        )
    }
}

/**
 * The live camera, its brightness behind the preview's graph sampled twice a second and smoothed like the record
 * screen.
 */
@Composable
private fun liveCamera(style: GraphStyle, camera: LiveCamera): LivePicture {
    var luma by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(camera) {
        val r = PreviewRect
        while (true) {
            camera.sampleLuma(r.x, r.y, r.w, r.h)?.let { now -> luma = luma?.let { it + (now - it) * 0.5f } ?: now }
            delay(500)
        }
    }
    return LivePicture(stringResource(Res.string.style_live_camera), luma) { modifier ->
        camera.picture(modifier, style.blursVideo)
    }
}

/** The video the editor came from, playing silently on a loop inside its trim. */
@Composable
private fun liveVideo(
    video: VideoItem,
    style: GraphStyle,
    frames: List<ImageBitmap>,
    viewModel: StyleEditorViewModel,
): LivePicture {
    val player =
        remember(video.id) {
            PlayerState().apply {
                muted = true
                seek(video.trimInUs)
                playing = true
            }
        }
    // Which of the few frames is shown changes a dozen times a loop; the position, every frame: only the first
    // recomposes.
    val index by remember(video, player, viewModel) { derivedStateOf { viewModel.frameAt(video, player.positionUs) } }
    val frame = frames.getOrNull(index)
    val luma = remember(frame) { frame?.let { averageLuma(it) } }
    val caption = stringResource(Res.string.style_your_video)
    return remember(caption, luma, video, player, style.blursVideo) {
        LivePicture(caption, luma) { modifier ->
            VideoSurface(
                file = viewModel.videoPath(video),
                state = player,
                clipUs = video.trimInUs..video.trimOutUs,
                modifier = modifier,
                drawFrames = style.blursVideo,
            )
        }
    }
}

/** The editor without a view model, so screenshot tests can draw every tab. */
@Composable
fun StyleEditorBody(
    draft: GraphStyle,
    frames: List<ImageBitmap>,
    onChange: ((GraphStyle) -> GraphStyle) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    live: LivePicture? = null,
    /** A style not saved yet: the editor opens on its name. */
    isNew: Boolean = false,
    initialTab: StyleTab = StyleTab.PANEL,
    sheetOverride: (@Composable () -> Unit)? = null,
    /** The style tile this editor was opened from, which its preview grows out of. */
    sharedKey: Any? = null,
) {
    var tab by remember { mutableStateOf(initialTab) }
    var frame by remember { mutableIntStateOf(0) }
    var color by remember { mutableStateOf<ColorTarget?>(null) }
    val image = frames.getOrNull(frame.coerceIn(0, (frames.size - 1).coerceAtLeast(0)))
    val stillLuma = remember(image) { image?.let { averageLuma(it) } ?: SCENE_LUMA }
    val luma = live?.luma ?: stillLuma
    val header =
        @Composable {
            Header(
                draft.name,
                onRename = { name -> onChange { it.copy(name = name) } },
                onBack,
                onSave,
                nameFirst = isNew,
            )
        }
    val controls =
        @Composable { m: Modifier ->
            Column(
                m.verticalScroll(rememberScrollState()).padding(CgSpace.S16),
                verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
            ) {
                // A tab's controls slide across from the side the tab sits on, the last ones out the other way,
                // clipped to the panel; rows that come and go inside move the height.
                AnimatedContent(
                    tab,
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        slideInHorizontally(CgMotion.offsetSpec) { dir * it }
                            .togetherWith(slideOutHorizontally(CgMotion.offsetSpec) { -dir * it })
                            .using(SizeTransform(clip = true) { _, _ -> CgMotion.sizeSpec })
                    },
                    label = "tab",
                ) { shown ->
                    Column(
                        Modifier.animateContentSize(CgMotion.sizeSpec),
                        verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
                    ) {
                        when (shown) {
                            StyleTab.PANEL -> PanelControls(draft, onChange) { color = it }
                            StyleTab.LINES -> LineControls(draft, onChange) { color = it }
                            StyleTab.GRID -> GridControls(draft, onChange) { color = it }
                            StyleTab.TEXT -> TextControls(draft, onChange) { color = it }
                            StyleTab.READINGS -> ReadingControls(draft, luma, onChange) { color = it }
                        }
                    }
                }
            }
        }
    // The save sheet, else a colour's: one slides up at a time, and back down; the last one stays while it leaves.
    var lastOverride by remember { mutableStateOf(sheetOverride) }
    if (sheetOverride != null) lastOverride = sheetOverride
    val sheet: Any? = if (sheetOverride != null) SaveSheetKey else color
    val sheets =
        @Composable {
            EditorSheetSlot(sheet) { shown ->
                if (shown === SaveSheetKey) lastOverride?.invoke()
                else (shown as? ColorTarget)?.let { ColorSheet(it, draft, onChange, onDone = { color = null }) }
            }
        }
    val layout = LocalWindowLayout.current
    if (layout.kind == LayoutKind.Wide) {
        // The whole frame beside the controls, the colour and save sheets inside the controls' panel (on a fold, the
        // header and frame on the left page, the tabs, controls and sheets on the right).
        BoxWithConstraints(modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding()) {
            WidePanes(
                layout,
                Modifier.fillMaxSize(),
                // On a medium window the two share it, as the adaptive scaffold split it; the wider panel on an
                // expanded
                // one.
                panelWidth = STYLE_PANEL_WIDTH.takeIf { maxWidth >= EXPANDED_WIDTH },
                // The panel meets the frame at a hairline, as on the canvas.
                gap = false,
                main = {
                    Column(Modifier.fillMaxSize()) {
                        header()
                        Preview(
                            draft,
                            image,
                            live,
                            luma,
                            frames.size,
                            onFrame = { d -> frame = (frame + d).coerceIn(0, (frames.size - 1).coerceAtLeast(0)) },
                            modifier = Modifier.weight(1f).padding(bottom = CgSpace.S16),
                            full = true,
                            sharedKey = sharedKey,
                        )
                    }
                },
                panel = {
                    Box(
                        Modifier.fillMaxSize().background(CgColors.Surface.Base).drawBehind {
                            drawRect(CgColors.Line.Hairline, Offset.Zero, Size(CgStroke.Hairline.toPx(), size.height))
                        }
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Tabs(tab, tall = true) { tab = it }
                            controls(Modifier.fillMaxWidth().weight(1f))
                        }
                        // The controls dim under a sheet; the frame beside stays in full view. A tap on them closes a
                        // colour.
                        AnimatedVisibility(
                            sheet != null,
                            enter = fadeIn(CgMotion.short()),
                            exit = fadeOut(CgMotion.short()),
                        ) {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(CgColors.Surface.Scrim.copy(alpha = 0.6f))
                                    .pointerInput(Unit) { detectTapGestures { color = null } }
                            )
                        }
                        Box(Modifier.align(Alignment.BottomCenter)) { sheets() }
                    }
                },
            )
        }
        return
    }
    Box(modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            header()
            Preview(
                draft,
                image,
                live,
                luma,
                frames.size,
                onFrame = { d -> frame = (frame + d).coerceIn(0, (frames.size - 1).coerceAtLeast(0)) },
                sharedKey = sharedKey,
            )
            Tabs(tab) { tab = it }
            controls(Modifier.fillMaxWidth().weight(1f))
        }
        Box(Modifier.align(Alignment.BottomCenter)) { sheets() }
    }
}

/**
 * The style's name, renamed in place: tap it to edit, the keyboard's done (or leaving the field) keeps it. A new style
 * or copy ([nameFirst]) opens with its name ready to type over.
 */
@Composable
private fun Header(
    name: String,
    onRename: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    nameFirst: Boolean = false,
) {
    var editing by remember { mutableStateOf(nameFirst) }
    // The whole name selected, so typing replaces "Aero copy" at once.
    // Keyed on the name: a new style's name is put into words a moment after the editor opens.
    var text by remember(name) { mutableStateOf(TextFieldValue(name, selection = TextRange(0, name.length))) }
    val focus = remember { FocusRequester() }
    val nameLabel = stringResource(Res.string.style_name)
    val keyboard = LocalSoftwareKeyboardController.current
    fun commit() {
        val trimmed = text.text.trim()
        if (trimmed.isNotEmpty() && trimmed != name) onRename(trimmed)
        editing = false
        keyboard?.hide()
    }
    Row(
        Modifier.fillMaxWidth()
            .drawBehind {
                val h = CgStroke.Hairline.toPx()
                drawRect(CgColors.Line.Hairline, Offset(0f, size.height - h), Size(size.width, h))
            }
            .padding(CgSpace.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack, modifier = Modifier.padding(end = CgSpace.S8))
        if (editing) {
            var focused by remember { mutableStateOf(false) }
            BasicTextField(
                text,
                onValueChange = { text = it.copy(text = it.text.take(32)) },
                singleLine = true,
                textStyle = CgTheme.type.label.copy(color = CgColors.Text.Primary),
                cursorBrush = SolidColor(CgColors.Brand.Signal),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                modifier =
                    Modifier.weight(1f)
                        .focusRequester(focus)
                        .onFocusChanged {
                            if (focused && !it.isFocused) commit()
                            focused = it.isFocused
                        }
                        .semantics { contentDescription = nameLabel },
            )
            LaunchedEffect(Unit) { focus.requestFocus() }
        } else {
            Row(
                Modifier.weight(1f)
                    .heightIn(min = CgSize.Touch)
                    .drawBehind { dashedRule(CgColors.Line.Hairline) }
                    .clickable(role = Role.Button, onClickLabel = stringResource(Res.string.style_rename)) {
                        text = TextFieldValue(name, selection = TextRange(0, name.length))
                        editing = true
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name.caps(),
                    style = CgTheme.type.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.weight(1f, fill = false).padding(horizontal = CgSpace.S4).semantics { heading() },
                )
                CgIcon(CgIcons.Edit, tint = CgColors.Text.Dim, size = 14.dp)
            }
        }
        // Where the bars are native (iPhone Duo) save is their prominent item, not a key here.
        val saveTitle = stringResource(Res.string.style_save)
        NativeItems(
            listOf(
                NativeBarItem(
                    TestTags.StyleEditorSave,
                    saveTitle,
                    BarIcon.Save,
                    prominent = true,
                    priority = BarPriority.High,
                )
            )
        ) {
            if (editing) commit()
            onSave()
        }
        if (LocalNativeChrome.current != null) return@Row
        Box(
            Modifier.padding(start = CgSpace.S8)
                .testTag(TestTags.StyleEditorSave)
                .heightIn(min = CgSize.Touch)
                .background(CgColors.Brand.Signal)
                .clickable(role = Role.Button) {
                    if (editing) commit()
                    onSave()
                }
                .padding(horizontal = CgSpace.S16),
            Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.style_save).caps(),
                style = CgTheme.type.labelStrong,
                color = CgColors.Text.OnLight,
            )
        }
    }
}

/**
 * 330 dp of the picture with the graph on it: the [live] picture when there is one, else a still frame of the video,
 * dragged sideways to try other frames.
 */
@Composable
private fun Preview(
    style: GraphStyle,
    image: ImageBitmap?,
    live: LivePicture?,
    luma: Float,
    frameCount: Int,
    onFrame: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** The whole 9:16 frame fitted to the space (a wide window), rather than the strip the graph sits on. */
    full: Boolean = false,
    sharedKey: Any? = null,
) {
    val haze = rememberHazeState()
    val drag = live == null && frameCount > 1
    // What the picture is and what dragging it does: inside the frame when the whole frame shows, else over the
    // strip.
    val caption =
        @Composable {
            Text(
                when {
                    live != null -> live.caption
                    frameCount > 1 -> stringResource(Res.string.style_preview_drag)
                    else -> stringResource(Res.string.style_preview_record)
                }.caps(),
                style = CgTheme.type.labelStrong,
                modifier =
                    Modifier.padding(CgSpace.S8)
                        .background(CgColors.Surface.Base.copy(alpha = 0.85f))
                        .border(CgStroke.Hairline, CgColors.Line.Hairline)
                        .padding(horizontal = CgSpace.S8, vertical = CgSpace.S4),
                color = CgColors.Text.Primary,
            )
        }
    // The drag outlives a recomposition (more frames loaded): it steps through the frames as they are now.
    val frameBy by rememberUpdatedState(onFrame)
    BoxWithConstraints(
        (if (full) modifier.fillMaxSize() else modifier.fillMaxWidth().height(PREVIEW_HEIGHT))
            // Grows out of the tile it was opened from. Drawn in its own layer: the picture under it may be a camera
            // or a video, and the graph's blur needs its source.
            .then(if (sharedKey != null) Modifier.shareBounds(sharedKey, overlay = false) else Modifier)
            .clipToBounds()
            .drawWithContent {
                drawContent()
                if (full) return@drawWithContent
                val h = CgStroke.Hairline.toPx()
                drawRect(CgColors.Line.Hairline, Offset(0f, size.height - h), Size(size.width, h))
            }
            .pointerInput(drag) {
                if (!drag) return@pointerInput
                var acc = 0f
                detectHorizontalDragGestures(onDragEnd = { acc = 0f }) { _, dx ->
                    acc += dx
                    val step = 24.dp.toPx()
                    while (acc > step) {
                        frameBy(-1)
                        acc -= step
                    }
                    while (acc < -step) {
                        frameBy(1)
                        acc += step
                    }
                }
            }
    ) {
        val frameH = maxWidth * 16f / 9f
        // The frame is 9:16; the strip shows the part the graph sits on, as in Figma.
        Box(
            if (full)
                Modifier.align(Alignment.Center)
                    .fillMaxHeight()
                    .padding(CgSpace.S24)
                    .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                    .border(CgStroke.Hairline, CgColors.Line.Hairline)
                    .padding(CgStroke.Hairline)
                    .clipToBounds()
            else
                Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(maxWidth, frameH)
                    .offset(y = -PREVIEW_SHIFT)
        ) {
            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                when {
                    live != null -> live.content(Modifier.fillMaxSize().clipToBounds())
                    image != null -> Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else -> CoffeeScene(Modifier.fillMaxSize(), progress = 0.7f)
                }
            }
            ShotOverlay(
                shot = SampleShots.zer0,
                videoTimeUs = PREVIEW_AT_US,
                shotStartUs = 0,
                settings = OverlaySettings(styleId = style.id, rect = PreviewRect),
                style = style,
                title = "${style.name} · Zer0",
                fonts = CgTheme.overlayFonts,
                modifier = Modifier.fillMaxSize(),
                hazeState = haze,
                backdropLuma = luma,
            )
            if (full) caption()
        }
        if (!full) caption()
    }
}

@Composable
private fun Tabs(
    tab: StyleTab,
    /** As tall as the header beside it (a wide window), so the two rules line up. */
    tall: Boolean = false,
    onTab: (StyleTab) -> Unit,
) {
    // The underline slides from tab to tab; drawn under the row, so only drawing follows it.
    val at = remember { Animatable(tab.ordinal.toFloat()) }
    LaunchedEffect(tab) { at.animateTo(tab.ordinal.toFloat(), CgMotion.settle()) }
    val count = StyleTab.entries.size
    Row(
        Modifier.fillMaxWidth().height(if (tall) HEADER_HEIGHT else CgSize.Touch).drawBehind {
            val w = size.width / count
            val line = CgStroke.Hairline.toPx()
            drawRect(CgColors.Line.Hairline, Offset(0f, size.height - line), Size(size.width, line))
            val h = CgStroke.Rule.toPx()
            drawRect(CgColors.Brand.Bone, Offset(w * at.value, size.height - h), Size(w, h))
        }
    ) {
        for (t in StyleTab.entries) {
            val on = t == tab
            val ink by animateColorAsState(if (on) CgColors.Text.Primary else CgColors.Text.Secondary, CgMotion.short())
            Box(
                Modifier.weight(1f).fillMaxSize().testTag(TestTags.styleTab(t.name.lowercase())).clickable(
                    role = Role.Tab
                ) {
                    onTab(t)
                },
                Alignment.Center,
            ) {
                Text(stringResource(t.label).caps(), style = CgTheme.type.labelStrong, color = ink, maxLines = 1)
            }
        }
    }
}

/** The save sheet's place in the sheet slot, beside the colour targets. */
private object SaveSheetKey

// Rows --------------------------------------------------------------------------------------

@Composable
internal fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    text: String,
    onChange: (Float) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CgSpace.S12),
    ) {
        StyleLabel(label, Modifier.width(SLIDER_LABEL))
        CgSlider(value.coerceIn(range), onChange, range, Modifier.weight(1f), label = label)
        Text(
            text.caps(),
            style = CgTheme.type.labelStrong,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(SLIDER_VALUE),
        )
    }
}

@Composable
internal fun Note(text: String) {
    Text(text, style = CgTheme.type.caption, color = CgColors.Text.Secondary)
}

// Tabs ----------------------------------------------------------------------------------------

/** What is hard to read over this frame, and a fix for it. The tabs' controls live in StyleControls.kt. */
@Composable
internal fun ReadabilityWarning(s: GraphStyle, luma: Float, change: ((GraphStyle) -> GraphStyle) -> Unit) {
    s.readabilityIssue(luma)?.let { issue ->
        Column(
            Modifier.fillMaxWidth()
                .border(CgStroke.Hairline, CgColors.Status.Warning)
                .padding(horizontal = CgSpace.S12, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(CgSpace.S6),
        ) {
            Text(
                stringResource(
                        Res.string.style_readability_title,
                        issue.reading.label().replaceFirstChar { it.uppercase() },
                    )
                    .caps(),
                style = CgTheme.type.labelStrong,
                color = CgColors.Status.Warning,
            )
            Text(
                stringResource(
                    if (s.panel.material == Material.SOLID) Res.string.style_readability_text_panel
                    else Res.string.style_readability_text_tint,
                    colorName(issue.value).text().replaceFirstChar { it.uppercase() },
                    "${(issue.ratio * 10).roundToInt() / 10f}",
                ),
                style = CgTheme.type.caption,
                color = CgColors.Text.Secondary,
            )
            // The whole 44 dp cell takes the tap; the amber key drawn in it is smaller.
            Box(
                Modifier.heightIn(min = CgSize.Touch)
                    .clickable(role = Role.Button) { change { it.fixReadability(luma) } }
                    .padding(top = CgSpace.S4),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    stringResource(Res.string.style_fix_it).caps(),
                    style = CgTheme.type.labelStrong,
                    color = CgColors.Text.OnLight,
                    modifier =
                        Modifier.heightIn(min = 36.dp)
                            .background(CgColors.Status.Warning)
                            .padding(horizontal = CgSpace.S12, vertical = 10.dp),
                )
            }
        }
    }
}

// Sheets ------------------------------------------------------------------------------------

/**
 * The canvas's colour sheet: the presets that work on video, then (for a panel's colour) the panel colours under their
 * label, each six to a row; a hue strip, hex, and opacity where the colour has one.
 */
@Composable
private fun ColorSheet(
    target: ColorTarget,
    s: GraphStyle,
    change: ((GraphStyle) -> GraphStyle) -> Unit,
    onDone: () -> Unit,
) {
    val current = target.get(s)
    val alpha = if (target.withOpacity) current.opacity else 1f
    fun set(c: Int) = change { target.set(it, c.withAlpha(alpha)) }
    EditorSheet(target.title.text(), onDone = onDone) {
        val panels = target.withOpacity && target.title != Res.string.style_grid_colour.text()
        SwatchGrid(Swatches, current) { set(it.color) }
        if (panels) {
            StyleLabel(stringResource(Res.string.style_panels))
            // Clear keeps a faint wash: white at the colour's own opacity, if that is faint already, else 16 %.
            SwatchGrid(PanelSwatches, current) { sw ->
                if (sw.clear) change { target.set(it, sw.color.withAlpha(min(alpha, CLEAR_OPACITY))) }
                else set(sw.color)
            }
        }
        HueStrip(current) { set(it) }
        // With an opacity, the hex carries it too (#RRGGBBAA), so any exact colour can be typed.
        var text by
            remember(current) { mutableStateOf(if (target.withOpacity) hexWithAlpha(current) else hex(current)) }
        Row(horizontalArrangement = Arrangement.spacedBy(CgSpace.S12), verticalAlignment = Alignment.CenterVertically) {
            StyleLabel(stringResource(Res.string.style_hex_colour), Modifier.width(SLIDER_LABEL))
            CgTextField(
                text,
                onValueChange = { t ->
                    text = t.take(if (target.withOpacity) 9 else 7)
                    if (target.withOpacity) parseHexWithAlpha(t)?.let { c -> change { target.set(it, c) } }
                    else parseHex(t)?.let { set(it) }
                },
                label = stringResource(Res.string.style_hex_colour),
                modifier = Modifier.weight(1f),
            )
        }
        if (target.withOpacity) {
            SliderRow(stringResource(Res.string.style_opacity), alpha, 0f..1f, "${(alpha * 100).roundToInt()}%") { v ->
                change { target.set(it, current.withAlpha(step(v, 0.01f).coerceIn(0f, 1f))) }
            }
        }
    }
}

/** [swatches] six to a row, square, the picked one framed in signal; clear drawn hatched. */
@Composable
private fun SwatchGrid(swatches: List<Swatch>, current: Int, onPick: (Swatch) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S8)) {
        for (row in swatches.chunked(SWATCH_COLUMNS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(CgSpace.S8)) {
                for (sw in row) {
                    val on = sw.matches(current)
                    val name = stringResource(sw.name)
                    Box(
                        Modifier.weight(1f)
                            .aspectRatio(1f)
                            .then(
                                if (sw.clear) Modifier.drawBehind { hatch() } else Modifier.background(Color(sw.color))
                            )
                            .border(
                                if (on) CgStroke.Rule else CgStroke.Hairline,
                                if (on) CgColors.Brand.Bone else CgColors.Line.Hairline,
                            )
                            .semantics {
                                contentDescription = name
                                selected = on
                            }
                            .clickable(role = Role.RadioButton) { onPick(sw) }
                    )
                }
                repeat(SWATCH_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The clear swatch: bars at 45° on the sheet's ground, as the canvas hatches "no colour". */
private fun DrawScope.hatch() {
    drawRect(CgColors.Surface.Bar)
    val step = 8.dp.toPx()
    val w = 4.dp.toPx() / sqrt(2f) * 2f
    clipRect {
        var x = -size.height
        while (x < size.width) {
            drawLine(CgColors.Line.Hairline, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = w / 2f)
            x += step
        }
    }
}

/** The hues across a strip; a tap or drag takes that hue at the colour's own saturation and brightness. */
@Composable
private fun HueStrip(current: Int, onPick: (Int) -> Unit) {
    val (_, s0, v0) = current.toHsv()
    val sat = if (s0 < 0.15f) 0.6f else s0
    val value = if (v0 < 0.3f) 0.9f else v0
    // The caller's pick carries the colour's current opacity: keep the gestures calling the latest one.
    val pickNow by rememberUpdatedState(onPick)
    Canvas(
        Modifier.fillMaxWidth()
            .height(32.dp)
            .border(CgStroke.Hairline, CgColors.Line.Hairline)
            .pointerInput(sat, value) {
                fun pick(x: Float) = pickNow(hsv((x / size.width).coerceIn(0f, 1f) * 360f, sat, value))
                detectTapGestures { pick(it.x) }
            }
            .pointerInput(sat, value) {
                detectHorizontalDragGestures { change, _ ->
                    pickNow(hsv((change.position.x / size.width).coerceIn(0f, 1f) * 360f, sat, value))
                }
            }
    ) {
        drawRect(Brush.horizontalGradient((0..6).map { Color(hsv(it * 60f, sat, value)) }))
    }
}

/** Figma 13 · G: name, where the style is the default, save. */
@Composable
fun SaveSheet(
    name: String,
    defaults: Set<SourceKind>,
    onSave: (String, Set<SourceKind>) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(name) }
    var picked by remember { mutableStateOf(defaults) }
    EditorSheet(stringResource(Res.string.style_save_style), onDone = null) {
        Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S6)) {
            StyleLabel(stringResource(Res.string.style_name))
            CgTextField(
                text,
                onValueChange = { text = it.take(32) },
                label = stringResource(Res.string.style_name),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        StyleLabel(stringResource(Res.string.style_use_by_default_for))
        Column(Modifier.fillMaxWidth().border(CgStroke.Hairline, CgColors.Line.Hairline)) {
            DefaultSources.forEachIndexed { i, (kind, label) ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(CgStroke.Hairline).background(CgColors.Line.Divider))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = CgSpace.S12, vertical = CgSpace.S6),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CgSpace.S12),
                ) {
                    Box(Modifier.size(width = 4.dp, height = 20.dp).background(machineColor(kind)))
                    Text(label.text().caps(), style = CgTheme.type.labelStrong, modifier = Modifier.weight(1f))
                    CgToggle(
                        kind in picked,
                        onCheckedChange = { on -> picked = if (on) picked + kind else picked - kind },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(CgSpace.S8)) {
            CgButton(
                stringResource(Res.string.style_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                kind = CgButtonKind.Secondary,
            )
            CgButton(
                stringResource(Res.string.style_save),
                onClick = { onSave(text, picked) },
                modifier = Modifier.weight(1f).testTag(TestTags.StyleSaveConfirm),
                kind = CgButtonKind.Signal,
                enabled = text.isNotBlank(),
            )
        }
    }
}

/**
 * The glass sliders of the Liquid demo app (github.com/fletchmckee/liquid), in its order, with its ranges, steps and
 * value formats. Every change shows in the preview at once (Haze) and in the export (the same shader).
 */
@Composable
internal fun GlassControls(g: GlassSettings, change: ((GlassSettings) -> GlassSettings) -> Unit) {
    fun whole(v: Float) = "${v.roundToInt()}"
    fun twoPlaces(v: Float): String {
        val hundredths = (v * 100).roundToInt()
        return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
    }
    // Steps as in the demo: the range split into that many equal intervals.
    fun step(v: Float, range: ClosedFloatingPointRange<Float>, intervals: Int): Float {
        val size = (range.endInclusive - range.start) / intervals
        return (range.start + ((v - range.start) / size).roundToInt() * size).coerceIn(range)
    }
    @Composable
    fun Demo(
        label: String,
        value: Float,
        range: ClosedFloatingPointRange<Float>,
        intervals: Int,
        text: String,
        set: GlassSettings.(Float) -> GlassSettings,
    ) = SliderRow(label, value, range, text) { v -> change { it.set(step(v, range, intervals)) } }

    Demo(stringResource(Res.string.style_glass_frost), g.frost, 0f..30f, 30, whole(g.frost)) { copy(frost = it) }
    Demo(stringResource(Res.string.style_glass_refraction), g.refraction, 0f..0.5f, 50, twoPlaces(g.refraction)) {
        copy(refraction = it)
    }
    Demo(stringResource(Res.string.style_glass_curve), g.curve, 0f..1f, 100, twoPlaces(g.curve)) { copy(curve = it) }
    Demo(stringResource(Res.string.style_glass_edge), g.edge, 0f..0.25f, 25, twoPlaces(g.edge)) { copy(edge = it) }
    Demo(stringResource(Res.string.style_glass_saturation), g.saturation, 0f..2f, 100, twoPlaces(g.saturation)) {
        copy(saturation = it)
    }
    Demo(stringResource(Res.string.style_glass_corner), g.corner, 0f..50f, 50, "${whole(g.corner)}%") {
        copy(corner = it)
    }
    Demo(stringResource(Res.string.style_glass_dispersion), g.dispersion, 0f..1f, 100, twoPlaces(g.dispersion)) {
        copy(dispersion = it)
    }
    Demo(stringResource(Res.string.style_glass_contrast), g.contrast, 0f..2f, 100, twoPlaces(g.contrast)) {
        copy(contrast = it)
    }
}

private fun snap(v: Float, step: Float): Float = (kotlin.math.round(v / step) * step).coerceIn(0f, 1f)

/** Mean luminance of [image]'s middle third, where the graph sits in the preview. */
private fun averageLuma(image: ImageBitmap): Float {
    val w = image.width
    val h = image.height
    if (w == 0 || h == 0) return SCENE_LUMA
    val top = h / 3
    val rows = (h / 3).coerceAtLeast(1)
    val buf = IntArray(w * rows)
    image.readPixels(buf, 0, top, w, rows)
    var sum = 0f
    var n = 0
    for (i in buf.indices step 7) {
        sum += relativeLuminance(buf[i])
        n++
    }
    return if (n == 0) SCENE_LUMA else sum / n
}

/** The sources a style can be the default for: the machines by their names, and "scale only". */
private val DefaultSources: List<Pair<SourceKind, UiText>> =
    listOf(
        SourceKind.GAGGIUINO to "Gaggiuino".raw(),
        SourceKind.GAGGIMATE to "GaggiMate".raw(),
        SourceKind.DECENT to "Decent".raw(),
        SourceKind.SCALE to Res.string.style_scale_only.text(),
    )

/** The drawn scene behind the graph is dark. */
private const val SCENE_LUMA = 0.06f

private val PREVIEW_HEIGHT = 330.dp

/** A slider row's name and value columns, as the canvas sets them. */
internal val SLIDER_LABEL = 88.dp
private val SLIDER_VALUE = 44.dp

/** A preset colour in the colour sheet. */
private const val SWATCH_COLUMNS = 6

/** The dashed rule under the style's name, which says it can be typed over. */
private fun DrawScope.dashedRule(color: Color) {
    val y = size.height - CgStroke.Hairline.toPx() / 2
    drawLine(
        color,
        Offset(0f, y),
        Offset(size.width, y),
        CgStroke.Hairline.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
    )
}

/** The controls' panel beside the preview on a wide window. */
private val STYLE_PANEL_WIDTH = 420.dp

/** From here a window is expanded and the panel takes [STYLE_PANEL_WIDTH]; below, half the width. */
private val EXPANDED_WIDTH = 840.dp

/** The header: a 44 dp key with 8 dp around it. */
private val HEADER_HEIGHT = CgSize.Touch + CgSpace.S8 * 2
private val PREVIEW_SHIFT = 120.dp

/** Where the graph sits in the 390 × 693 frame: 14 px in, under the shift, 170 tall — Figma's layout. */
private val PreviewRect = NormRect(14f / 390f, (120f + 130f) / 693f, 362f / 390f, 170f / 693f)

private const val PREVIEW_AT_US = 24_000_000L
