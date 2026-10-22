package app.onlyshots.open

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.onlyshots.PRIVACY_URL
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
import app.onlyshots.core.designsystem.component.CgIcon
import app.onlyshots.core.designsystem.component.LocalNativeChrome
import app.onlyshots.core.designsystem.component.OnlyShotsMark
import app.onlyshots.core.designsystem.headline
import app.onlyshots.core.model.NormRect
import app.onlyshots.core.model.OverlaySettings
import app.onlyshots.core.model.VideoItem
import app.onlyshots.core.model.sample.SampleShots
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.UiText
import app.onlyshots.core.resources.app_name
import app.onlyshots.core.resources.illus_shot_tile
import app.onlyshots.core.resources.illus_video_tile
import app.onlyshots.core.resources.open_change
import app.onlyshots.core.resources.open_details_does
import app.onlyshots.core.resources.open_details_does_title
import app.onlyshots.core.resources.open_details_shots
import app.onlyshots.core.resources.open_details_shots_machine
import app.onlyshots.core.resources.open_details_shots_title
import app.onlyshots.core.resources.open_details_videos
import app.onlyshots.core.resources.open_details_videos_title
import app.onlyshots.core.resources.open_drop_to_use
import app.onlyshots.core.resources.open_editor
import app.onlyshots.core.resources.open_hint_add_shot
import app.onlyshots.core.resources.open_hint_add_video
import app.onlyshots.core.resources.open_hint_both
import app.onlyshots.core.resources.open_hint_ready
import app.onlyshots.core.resources.open_preview_caption
import app.onlyshots.core.resources.open_privacy
import app.onlyshots.core.resources.open_recorded
import app.onlyshots.core.resources.open_shot_weight_in
import app.onlyshots.core.resources.open_video
import app.onlyshots.core.resources.open_wide_choose_shot
import app.onlyshots.core.resources.open_wide_choose_video
import app.onlyshots.core.resources.open_wide_intro
import app.onlyshots.core.resources.open_wide_shot_detail
import app.onlyshots.core.resources.open_wide_shot_detail_machine
import app.onlyshots.core.resources.open_wide_shot_title
import app.onlyshots.core.resources.open_wide_video_drop
import app.onlyshots.core.resources.open_wide_video_title
import app.onlyshots.core.resources.photo_shot
import app.onlyshots.core.resources.start_title
import app.onlyshots.core.resources.text
import app.onlyshots.core.resources.unit_grams
import app.onlyshots.core.resources.unit_seconds
import app.onlyshots.machineLabel
import app.onlyshots.overlay.GraphStyles
import app.onlyshots.overlay.compose.ShotOverlay
import app.onlyshots.shareBounds
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the desktop or the web says about itself on its start screen. */
data class OpenHost(
    /** In the header, beside the shield: where it runs. */
    val headerNote: String,
    /** The "videos" column: what comes out, and where. */
    val videosNote: String,
)

/**
 * The desktop's and the web's start, from the canvas's "Open-Desk" artboard: on a faint grid, a header with where it
 * runs, the title and the two steps as cells beside a preview of what comes out, "open editor", and what it does, reads
 * and makes in a hairline grid underneath. Narrow, the preview goes under the steps, and the steps and notes stack.
 */
@Composable
internal fun WideOpenBody(
    picked: VideoItem?,
    pickedFile: PickedFile?,
    shot: ChosenShot?,
    status: UiText?,
    onPickVideo: () -> Unit,
    onPickShot: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier,
    dragging: Boolean,
    fromMachine: Boolean,
    host: OpenHost?,
    stores: StoreLinks?,
    onPrivacy: (() -> Unit)?,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding()) {
        // A phone's browser gets the design's own compact page, with the app's banner.
        if (stores?.phone == true && maxWidth < 600.dp) {
            MobileOpenBody(
                picked,
                pickedFile,
                shot,
                status,
                onPickVideo,
                onPickShot,
                onOpenEditor,
                stores,
                host?.videosNote,
            )
            return@BoxWithConstraints
        }
        val w = maxWidth.value
        val gutter = (w * 0.044f).coerceIn(16f, 64f).dp
        val wide = maxWidth >= 900.dp
        val columns = maxWidth >= 800.dp
        Column(Modifier.fillMaxSize().drawBehind { grid() }.verticalScroll(rememberScrollState())) {
            Header(host?.headerNote, gutter, narrow = !columns, onPrivacy = onPrivacy)
            Column(
                Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 1312.dp)
                    .padding(start = gutter, end = gutter, top = (w * 0.033f).coerceIn(24f, 48f).dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                val steps =
                    @Composable { mod: Modifier ->
                        Steps(
                            picked,
                            pickedFile,
                            shot,
                            status,
                            onPickVideo,
                            onPickShot,
                            onOpenEditor,
                            dragging,
                            fromMachine,
                            twoUp = columns,
                            modifier = mod,
                        )
                    }
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy((w * 0.044f).coerceIn(32f, 64f).dp)) {
                        steps(Modifier.weight(1f))
                        Preview(Modifier.width(300.dp))
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        steps(Modifier.fillMaxWidth())
                        Preview(Modifier.width(300.dp))
                    }
                }
                // The web offers the phone app: it films the shot live, which the page cannot.
                if (stores?.any == true) AppBand(stores, wide = columns)
                Details(fromMachine, host?.videosNote, columns = columns)
            }
        }
    }
}

/** The page's faint 24 dp grid, as the canvas draws the desk's ground. */
private fun DrawScope.grid() {
    val step = 24.dp.toPx()
    val line = CgColors.Text.Primary.copy(alpha = 0.035f)
    var x = 0f
    while (x < size.width) {
        drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}

/** The name, where it runs, and the privacy link; narrow, the last two go under the name. */
@Composable
private fun Header(note: String?, gutter: Dp, narrow: Boolean, onPrivacy: (() -> Unit)?) {
    val uri = LocalUriHandler.current
    val brand =
        @Composable {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OnlyShotsMark(splashTarget = true)
                Text(
                    stringResource(Res.string.app_name).caps(),
                    style = CgTheme.type.sheetTitle.copy(letterSpacing = 0.08.em),
                    color = CgColors.Text.Primary,
                )
            }
        }
    val meta =
        @Composable {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (note != null) {
                    Row(
                        Modifier.weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CgIcon(CgIcons.Shield, tint = CgColors.Text.Secondary, size = 16.dp)
                        Text(note, style = CgTheme.type.caption, color = CgColors.Text.Secondary)
                    }
                }
                Text(
                    stringResource(Res.string.open_privacy),
                    style =
                        CgTheme.type.labelStrong.copy(letterSpacing = 0.em, textDecoration = TextDecoration.Underline),
                    color = CgColors.Text.Primary,
                    modifier =
                        Modifier.heightIn(min = CgSize.Touch).wrapContentHeight().clickable(role = Role.Button) {
                            onPrivacy?.invoke() ?: uri.openUri(PRIVACY_URL)
                        },
                )
            }
        }
    Column {
        if (narrow) {
            Column(Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 14.dp)) {
                brand()
                meta()
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = gutter, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(CgSpace.S16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                brand()
                Box(Modifier.weight(1f))
                meta()
            }
        }
        Box(Modifier.fillMaxWidth().height(CgStroke.Hairline).background(CgColors.Line.Hairline))
    }
}

/** The title, the two steps (side by side when [twoUp]), and "open editor" with what it is waiting for. */
@Composable
private fun Steps(
    picked: VideoItem?,
    pickedFile: PickedFile?,
    shot: ChosenShot?,
    status: UiText?,
    onPickVideo: () -> Unit,
    onPickShot: () -> Unit,
    onOpenEditor: () -> Unit,
    dragging: Boolean,
    fromMachine: Boolean,
    twoUp: Boolean,
    modifier: Modifier,
) {
    val opening = status?.takeIf { it == Opening }?.text()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            stringResource(Res.string.start_title).headline(),
            style = CgTheme.type.valueLarge.copy(letterSpacing = (-0.02).em, lineHeight = 48.sp),
            color = CgColors.Text.Primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(Res.string.open_wide_intro),
            style = CgTheme.type.body.copy(lineHeight = 24.sp),
            color = CgColors.Text.Secondary,
            modifier = Modifier.widthIn(max = 640.dp),
        )
        val video =
            @Composable { mod: Modifier ->
                StepSwap(picked != null, mod) { done ->
                    if (!done || picked == null) {
                        DeskEmptyStep(
                            number = 1,
                            title = stringResource(Res.string.open_wide_video_title),
                            detail =
                                opening
                                    ?: stringResource(
                                        if (dragging) Res.string.open_drop_to_use else Res.string.open_wide_video_drop
                                    ),
                            action = stringResource(Res.string.open_wide_choose_video),
                            next = true,
                            highlighted = dragging,
                            onClick = onPickVideo,
                            tag = TestTags.OpenPickVideo,
                        )
                    } else {
                        DeskPickedStep(
                            number = 1,
                            title = stringResource(Res.string.open_wide_video_title),
                            name = pickedFile?.name ?: stringResource(Res.string.open_video),
                            detail = videoLine(picked, pickedFile),
                            tile = { VideoTile(pickedFile?.thumb, picked.id, Modifier.size(64.dp, 112.dp)) },
                            onChange = onPickVideo,
                            highlighted = dragging,
                            tag = TestTags.OpenPickVideo,
                        )
                    }
                }
            }
        val shotStep =
            @Composable { mod: Modifier ->
                StepSwap(shot != null, mod) { done ->
                    if (!done || shot == null) {
                        DeskEmptyStep(
                            number = 2,
                            title = stringResource(Res.string.open_wide_shot_title),
                            detail =
                                stringResource(
                                    if (fromMachine) Res.string.open_wide_shot_detail_machine
                                    else Res.string.open_wide_shot_detail
                                ),
                            action = stringResource(Res.string.open_wide_choose_shot),
                            next = picked != null,
                            highlighted = false,
                            onClick = onPickShot,
                            tag = TestTags.OpenPickShot,
                        )
                    } else {
                        DeskPickedStep(
                            number = 2,
                            title = stringResource(Res.string.open_wide_shot_title),
                            name = shot.label,
                            detail = shotLine(shot),
                            tile = { ShotTile() },
                            onChange = onPickShot,
                            highlighted = false,
                            tag = TestTags.OpenPickShot,
                        )
                    }
                }
            }
        if (twoUp) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(CgSpace.S16)) {
                video(Modifier.weight(1f).fillMaxHeight())
                shotStep(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S12)) {
                video(Modifier.fillMaxWidth())
                shotStep(Modifier.fillMaxWidth())
            }
        }
        status?.takeIf { it != Opening }?.let { StatusLine(it.text()) }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            OpenEditorKey(ready = picked != null && shot != null, onClick = onOpenEditor)
            Text(
                stringResource(ctaHint(picked != null, shot != null)),
                style = CgTheme.type.bodySmall,
                color = CgColors.Text.Secondary,
            )
        }
    }
}

/** A step not done yet, on the desk: a dashed cell (signal when it is next), its number and name, and its key. */
@Composable
private fun DeskEmptyStep(
    number: Int,
    title: String,
    detail: String,
    action: String,
    next: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    val accent =
        if (highlighted) CgColors.Brand.Signal else if (next) CgColors.Text.Primary else CgColors.Text.Secondary
    Column(
        Modifier.fillMaxSize()
            .background(if (highlighted) CgColors.Surface.Raised else Color.Transparent)
            .then(
                if (highlighted) Modifier.border(CgStroke.Rule, CgColors.Brand.Signal)
                else Modifier.drawBehind { dashedBorder(CgColors.Line.Hairline) }
            )
            .padding(CgSpace.S16),
        // The words at the top, the key in the bottom corner: side by side, both cells' keys line up.
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.padding(bottom = CgSpace.S8), verticalArrangement = Arrangement.spacedBy(CgSpace.S8)) {
            Text("${stepNumber(number)} · ${title.caps()}", style = CgTheme.type.data, color = accent)
            Text(detail, style = CgTheme.type.body, color = CgColors.Text.Secondary)
        }
        StepKey(action, signal = next, onClick = onClick, modifier = Modifier.testTag(tag), shine = next)
    }
}

/** A step done, on the desk: a small picture of what was picked, its name and line, and "change". */
@Composable
private fun DeskPickedStep(
    number: Int,
    title: String,
    name: String,
    detail: String,
    tile: @Composable () -> Unit,
    onChange: () -> Unit,
    highlighted: Boolean,
    tag: String? = null,
) {
    Row(
        Modifier.fillMaxSize()
            .background(if (highlighted) CgColors.Surface.Raised else Color.Transparent)
            .border(
                if (highlighted) CgStroke.Rule else CgStroke.Hairline,
                if (highlighted) CgColors.Brand.Signal else CgColors.Line.Hairline,
            )
            .padding(CgSpace.S16),
        horizontalArrangement = Arrangement.spacedBy(CgSpace.S16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tile()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CgSpace.S4)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CgSpace.S6),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${stepNumber(number)} · ${title.caps()}",
                    style = CgTheme.type.data,
                    color = CgColors.Status.Success,
                )
                CgIcon(CgIcons.Check, tint = CgColors.Status.Success, size = 14.dp)
            }
            Text(
                name,
                style = CgTheme.type.label.copy(letterSpacing = 0.em),
                color = CgColors.Text.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(detail, style = CgTheme.type.data, color = CgColors.Text.Secondary)
        }
        StepKey(
            stringResource(Res.string.open_change),
            signal = false,
            onClick = onChange,
            modifier = if (tag != null) Modifier.testTag(tag) else Modifier,
        )
    }
}

/**
 * A step's key: 44 dp, Mono capitals; the signal fill when it is the next thing to do, else a hairline outline. Used by
 * every Open layout. Without [onClick] it only shows the way: the whole step row is what takes the tap.
 */
@Composable
internal fun StepKey(
    text: String,
    signal: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** A light runs over it now and then, like a shine on metal: the key to press next. */
    shine: Boolean = false,
) {
    Box(
        modifier
            .heightIn(min = CgSize.Touch)
            .background(if (signal) CgColors.Brand.Signal else Color.Transparent)
            .then(if (shine) Modifier.shine() else Modifier)
            .border(CgStroke.Hairline, if (signal) CgColors.Brand.Signal else CgColors.Line.Hairline)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp),
        Alignment.Center,
    ) {
        Text(
            text.caps(),
            style = CgTheme.type.labelStrong.copy(letterSpacing = 0.em),
            color = if (signal) CgColors.Text.OnLight else CgColors.Text.Primary,
        )
    }
}

/**
 * A soft light that crosses the key at a slant every few seconds and is gone between: a shine on metal. It starts off
 * the left edge and waits off the right, so a still frame (an IDE preview, a screenshot) shows the key plain.
 */
@Composable
private fun Modifier.shine(): Modifier {
    if (LocalInspectionMode.current) return this
    // A wait, a run across, back to the start: nothing draws while it waits, so the screen can go idle.
    val at = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(SHINE_REST_MS.toLong())
            at.animateTo(1f, tween(SHINE_PERIOD_MS - SHINE_REST_MS, easing = LinearEasing))
            at.snapTo(0f)
        }
    }
    // Its own layer, so only the key redraws; the light made once per size and moved.
    return graphicsLayer().clipToBounds().drawWithCache {
        val band = size.height * 2f
        val light =
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                start = Offset(-band / 2f, 0f),
                end = Offset(band / 2f, size.height),
            )
        onDrawWithContent {
            drawContent()
            val x = -band + (size.width + band * 2f) * at.value
            translate(left = x) { drawRect(light, topLeft = Offset(-x, 0f), size = size) }
        }
    }
}

/** A shine every 3.2 s: 2.4 s at rest, then 0.8 s across. */
private const val SHINE_PERIOD_MS = 3_200
private const val SHINE_REST_MS = 2_400

/**
 * "Open editor →": the signal key once the video and the shot are picked, a hairline outline with grey letters before.
 * [fill] stretches it over the width with the arrow at the far end (the phones); otherwise it is as wide as its words.
 */
@Composable
internal fun OpenEditorKey(ready: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, fill: Boolean = false) {
    // Where the bars are native (iPhone Duo) opening the editor is their prominent item.
    if (LocalNativeChrome.current != null) return
    // Read while drawing: the change of colour redraws the key, it does not recompose it.
    val ground = animateColorAsState(if (ready) CgColors.Brand.Signal else Color.Transparent, CgMotion.short())
    val ink by animateColorAsState(if (ready) CgColors.Text.OnLight else CgColors.Text.Disabled, CgMotion.short())
    Row(
        modifier
            .testTag(TestTags.OpenEditor)
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 56.dp)
            .drawBehind { drawRect(ground.value) }
            .border(CgStroke.Hairline, if (ready) CgColors.Brand.Signal else CgColors.Line.Hairline)
            .clickable(enabled = ready, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (fill) 20.dp else 24.dp),
        horizontalArrangement = if (fill) Arrangement.SpaceBetween else Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.open_editor).caps(), style = CgTheme.type.label, color = ink)
        CgIcon(CgIcons.ArrowRight, tint = ink, size = 18.dp)
    }
}

/**
 * Why the last pick did not take (a video it cannot read, a shot file it cannot parse): Mono, in the warning colour.
 */
@Composable
internal fun StatusLine(text: String) {
    Text(text, style = CgTheme.type.labelStrong.copy(letterSpacing = 0.em), color = CgColors.Status.Warning)
}

/** "01", "02": a step's number as the canvas sets it. */
internal fun stepNumber(number: Int): String = number.toString().padStart(2, '0')

/** What comes out: a 9:16 frame with the cup under the spout and the graph's card over it. */
@Composable
private fun Preview(modifier: Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
    ) {
        Box(Modifier.size(300.dp, 533.dp).border(CgStroke.Hairline, CgColors.Line.Hairline).clipToBounds()) {
            Image(
                painterResource(Res.drawable.photo_shot),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // The graph as the app draws it and the export writes it: Instrument, the sample shot, near its end.
            val style = GraphStyles.byId(PREVIEW_STYLE) ?: GraphStyles.all.first()
            ShotOverlay(
                shot = SampleShots.zer0,
                videoTimeUs = PREVIEW_AT_US,
                shotStartUs = 0L,
                settings = OverlaySettings(styleId = style.id, rect = PREVIEW_RECT),
                style = style,
                title = PREVIEW_TITLE,
                fonts = CgTheme.overlayFonts,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            stringResource(Res.string.open_preview_caption),
            style = CgTheme.type.caption,
            color = CgColors.Text.Secondary,
        )
    }
}

/** The preview's graph: its style, the moment of the sample shot it shows, and its title (proper nouns). */
private const val PREVIEW_STYLE = "instrument"
private const val PREVIEW_AT_US = 30_000_000L
private const val PREVIEW_TITLE = "Zer0 · Gaggiuino"
/** Up over the group head, so the cup and the stream under it stay in view. */
private val PREVIEW_RECT = NormRect.Default.copy(y = 60f / NormRect.REF_H)

/** What it does, which shots it reads, which videos it takes: three cells in a hairline grid. */
@Composable
private fun Details(fromMachine: Boolean, videosNote: String?, columns: Boolean) {
    val notes =
        listOf(
            stringResource(Res.string.open_details_does_title) to stringResource(Res.string.open_details_does),
            stringResource(Res.string.open_details_shots_title) to
                stringResource(
                    if (fromMachine) Res.string.open_details_shots_machine else Res.string.open_details_shots
                ),
            stringResource(Res.string.open_details_videos_title) to
                (stringResource(Res.string.open_details_videos) + (videosNote?.let { " $it" } ?: "")),
        )
    val note =
        @Composable { title: String, text: String, mod: Modifier ->
            Column(
                mod.background(CgColors.Surface.Base).padding(CgSpace.S16),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    title.caps(),
                    style = CgTheme.type.labelStrong.copy(letterSpacing = 0.em),
                    color = CgColors.Text.Secondary,
                    modifier = Modifier.semantics { heading() },
                )
                Text(text, style = CgTheme.type.bodySmall.copy(lineHeight = 20.sp), color = CgColors.Text.Primary)
            }
        }
    val frame = Modifier.fillMaxWidth().background(CgColors.Line.Hairline).padding(CgStroke.Hairline)
    if (columns) {
        Row(
            frame.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(CgStroke.Hairline),
        ) {
            for ((title, text) in notes) note(title, text, Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(frame, verticalArrangement = Arrangement.spacedBy(CgStroke.Hairline)) {
            for ((title, text) in notes) note(title, text, Modifier.fillMaxWidth())
        }
    }
}

// Small drawings from the design -----------------------------------------------------------------

/**
 * The video's tile: a frame of it, cropped to fill, in a hairline frame; until that is decoded (or where it cannot be),
 * the design's drawing of a cup under the spout with the graph's strip along the bottom.
 */
@Composable
internal fun VideoTile(
    thumb: ImageBitmap? = null,
    videoId: String? = null,
    modifier: Modifier = Modifier.size(54.dp, 72.dp),
) {
    // The editor's picture grows out of this tile when the editor opens on the video.
    val shared = if (videoId != null) Modifier.shareBounds(SharedKeys.video(videoId)) else Modifier
    // The frame fades in over the drawing once it is decoded.
    Crossfade(
        thumb,
        shared.then(modifier).border(CgStroke.Hairline, CgColors.Line.Hairline),
        CgMotion.medium(),
        label = "thumb",
    ) { frame ->
        if (frame != null) {
            Image(frame, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Image(
                painterResource(Res.drawable.illus_video_tile),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** The shot's tile: two curves, pressure and flow, in a hairline square. */
@Composable
internal fun ShotTile() {
    Box(
        Modifier.size(64.dp).background(CgColors.Surface.Raised).border(CgStroke.Hairline, CgColors.Line.Hairline),
        Alignment.Center,
    ) {
        Image(painterResource(Res.drawable.illus_shot_tile), contentDescription = null, Modifier.size(44.dp, 30.dp))
    }
}

/** A dashed hairline inside the bounds, square, as the canvas draws a step still to do. */
internal fun DrawScope.dashedBorder(color: Color) {
    val w = CgStroke.Hairline.toPx()
    drawRect(
        color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
    )
}

/** "0:42 · 1080 × 1920 · recorded 2026-10-20 08:14". */
@Composable
internal fun videoLine(item: VideoItem, file: PickedFile?): String =
    listOfNotNull(
            length(item.durationUs),
            file?.takeIf { it.widthPx > 0 && it.heightPx > 0 }?.let { "${it.widthPx} × ${it.heightPx}" },
            item.recordedAt?.let { stringResource(Res.string.open_recorded, it.toString().take(16).replace('T', ' ')) },
        )
        .joinToString(" · ")

/** "Gaggiuino · 36.4 g in 28 s · 2026-10-20 08:14". */
@Composable
internal fun shotLine(chosen: ChosenShot): String {
    val shot = chosen.shot
    val time = "${shot.durationUs / 1_000_000} ${stringResource(Res.string.unit_seconds)}"
    val grams = stringResource(Res.string.unit_grams)
    return listOf(
            machineLabel(shot.source),
            shot.finalWeight?.let {
                stringResource(Res.string.open_shot_weight_in, "${(it * 10).roundToInt() / 10f} $grams", time)
            } ?: time,
            shot.startedAt.toString().take(16).replace('T', ' '),
        )
        .joinToString(" · ")
}

internal fun length(us: Long): String {
    val seconds = us / 1_000_000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** What "open editor" is waiting for, said beside it. */
internal fun ctaHint(video: Boolean, shot: Boolean): StringResource =
    when {
        video && shot -> Res.string.open_hint_ready
        video -> Res.string.open_hint_add_shot
        shot -> Res.string.open_hint_add_video
        else -> Res.string.open_hint_both
    }

/** An empty step and the same step picked: the one ticks over to the other, the column moving to its new height. */
@Composable
internal fun StepSwap(
    done: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (done: Boolean) -> Unit,
) {
    AnimatedContent(
        done,
        modifier,
        transitionSpec = { with(CgMotion) { tick() } },
        label = "step",
    ) {
        content(it)
    }
}
