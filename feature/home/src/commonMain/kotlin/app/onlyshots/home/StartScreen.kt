package app.onlyshots.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.onlyshots.TestTags
import app.onlyshots.core.designsystem.CgColors
import app.onlyshots.core.designsystem.CgIcons
import app.onlyshots.core.designsystem.CgSize
import app.onlyshots.core.designsystem.CgSpace
import app.onlyshots.core.designsystem.CgStroke
import app.onlyshots.core.designsystem.CgTheme
import app.onlyshots.core.designsystem.caps
import app.onlyshots.core.designsystem.component.BarIcon
import app.onlyshots.core.designsystem.component.BluetoothBlue
import app.onlyshots.core.designsystem.component.CgSheet
import app.onlyshots.core.designsystem.component.ChoiceCard
import app.onlyshots.core.designsystem.component.IconCircleButton
import app.onlyshots.core.designsystem.component.LocalNativeChrome
import app.onlyshots.core.designsystem.component.NativeBarItem
import app.onlyshots.core.designsystem.component.NativeItems
import app.onlyshots.core.designsystem.component.OnlyShotsMark
import app.onlyshots.core.designsystem.component.SheetHeader
import app.onlyshots.core.designsystem.component.topRule
import app.onlyshots.core.designsystem.headline
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.app_name
import app.onlyshots.core.resources.home_about
import app.onlyshots.core.resources.home_start_edit_detail
import app.onlyshots.core.resources.home_start_edit_title
import app.onlyshots.core.resources.home_start_graph_series
import app.onlyshots.core.resources.home_start_question
import app.onlyshots.core.resources.home_start_record_detail
import app.onlyshots.core.resources.home_start_record_title
import app.onlyshots.core.resources.privacy_title
import app.onlyshots.core.resources.start_title
import app.onlyshots.layout.LayoutKind
import app.onlyshots.layout.LocalWindowLayout
import app.onlyshots.layout.PANEL_WIDTH
import app.onlyshots.layout.WidePanes
import org.jetbrains.compose.resources.stringResource

/**
 * The phones' first screen: record a shot live (Home, then the camera) or compose a video from one already filmed (pick
 * it and its shot file, then the editor). The ? opens the same sheet as Home's.
 */
@Composable
fun StartScreen(onRecord: () -> Unit, onEdit: () -> Unit, onPrivacy: (() -> Unit)? = null) {
    var help by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.home_about)
    val content: @Composable () -> Unit = {
        HelpContent(
            onPrivacy =
                onPrivacy?.let { open ->
                    {
                        help = false
                        open()
                    }
                }
        )
    }
    // Where the bars are native (iPhone Duo) help and privacy are its items, not keys on the screen.
    val privacyTitle = stringResource(Res.string.privacy_title)
    NativeItems(
        buildList {
            add(NativeBarItem(TestTags.StartHelp, title, BarIcon.Help, group = 1))
            if (onPrivacy != null) add(NativeBarItem(TestTags.HelpPrivacy, privacyTitle, BarIcon.Privacy, group = 1))
        }
    ) { id ->
        if (id == TestTags.StartHelp) help = true else onPrivacy?.invoke()
    }
    // On a book foldable the help takes the right page, in place of the choices; elsewhere it is a sheet.
    val fold = LocalWindowLayout.current.fold
    StartBody(
        onRecord = onRecord,
        onEdit = onEdit,
        onHelp = { help = true },
        helpPage =
            if (help && fold) {
                {
                    Column(Modifier.fillMaxSize().topRule().padding(top = CgStroke.Rule)) {
                        SheetHeader(title, onDone = { help = false })
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(CgSpace.S16)) {
                            content()
                        }
                    }
                }
            } else {
                null
            },
    )
    if (help && !fold) CgSheet(title = title, onDismiss = { help = false }) { content() }
}

/**
 * The start screen from plain callbacks, so the screenshot tests can draw it. A phone has one column, the choices at
 * the bottom where the thumb is. A wide window turned on its side puts the headline on the left and the choices in a
 * panel on the right (one page each on a book foldable); an upright one stays a column, the two choices side by side.
 */
@Composable
fun StartBody(
    onRecord: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    onHelp: () -> Unit = {},
    /** On a book foldable, the help as the right page, in place of the choices. */
    helpPage: (@Composable () -> Unit)? = null,
) {
    val layout = LocalWindowLayout.current
    BoxWithConstraints(modifier.fillMaxSize().background(CgColors.Surface.Base).drawBehind { drawGrid() }) {
        when {
            layout.kind != LayoutKind.Wide -> PhoneStart(onRecord, onEdit, onHelp)
            layout.fold || maxWidth > maxHeight ->
                WidePanes(
                    layout,
                    panelWidth = PANEL_WIDTH,
                    gap = false,
                    main = {
                        Column(
                            Modifier.fillMaxSize()
                                .safeDrawingPadding()
                                .padding(horizontal = if (layout.fold) CgSpace.S16 else 48.dp)
                                .padding(top = if (layout.fold) 48.dp else 40.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp),
                        ) {
                            StartHeader(onHelp = null, big = !layout.fold)
                            GraphCard(Modifier.weight(1f), big = !layout.fold)
                            Headline(big = !layout.fold)
                        }
                    },
                ) {
                    if (layout.fold && helpPage != null) {
                        helpPage()
                        return@WidePanes
                    }
                    Column(
                        Modifier.fillMaxSize()
                            .drawBehind {
                                drawLine(
                                    CgColors.Line.Hairline,
                                    Offset.Zero,
                                    Offset(0f, size.height),
                                    CgStroke.Hairline.toPx(),
                                )
                            }
                            .safeDrawingPadding()
                            .padding(horizontal = if (layout.fold) CgSpace.S16 else CgSpace.S24)
                            .padding(top = if (layout.fold) 48.dp else 40.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { HelpKey(onHelp) }
                        Spacer(Modifier.weight(1f))
                        Choices(onRecord, onEdit, sideBySide = false)
                    }
                }
            else ->
                Column(
                    Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 40.dp, vertical = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    StartHeader(onHelp = onHelp, big = true)
                    GraphCard(Modifier.weight(1f), big = true)
                    Headline(big = true)
                    Choices(onRecord, onEdit, sideBySide = true)
                }
        }
    }
}

/** The phone's start: one column, the headline scrolling if the window is short, the choices at the bottom. */
@Composable
private fun PhoneStart(onRecord: () -> Unit, onEdit: () -> Unit, onHelp: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding(), Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize().padding(horizontal = CgSpace.S16)) {
            StartHeader(onHelp = onHelp, big = false)
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CgSpace.S24),
            ) {
                Spacer(Modifier.height(0.dp))
                GraphCard(Modifier.height(GRAPH_CARD_PHONE), big = false)
                Headline(big = false)
            }
            Box(Modifier.padding(top = CgSpace.S16, bottom = CgSpace.S24)) {
                Choices(onRecord, onEdit, sideBySide = false)
            }
        }
    }
}

/** The mark (where the splash lands) and the name; the help key at the end when [onHelp] is given. */
@Composable
private fun StartHeader(onHelp: (() -> Unit)?, big: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(if (big) CgSize.Touch else 60.dp),
        horizontalArrangement = Arrangement.spacedBy(if (big) CgSpace.S12 else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnlyShotsMark(splashTarget = true)
        Text(
            stringResource(Res.string.app_name).caps(),
            style =
                (if (big) CgTheme.type.screenTitle else CgTheme.type.label).copy(
                    letterSpacing = CgTheme.type.labelStrong.letterSpacing
                ),
            modifier = Modifier.weight(1f),
        )
        if (onHelp != null) HelpKey(onHelp)
    }
}

@Composable
private fun HelpKey(onHelp: () -> Unit) {
    if (LocalNativeChrome.current != null) return
    IconCircleButton(
        CgIcons.Help,
        stringResource(Res.string.home_about),
        onClick = onHelp,
        modifier = Modifier.testTag(TestTags.StartHelp),
    )
}

/**
 * "FILM THE SHOT. KEEP THE DATA.": the headline, one sentence a line as the link banner sets it; the large Mono one in
 * a wide window.
 */
@Composable
private fun Headline(big: Boolean) {
    Text(
        stringResource(Res.string.start_title).headline(),
        style =
            if (big) {
                CgTheme.type.valueLarge.copy(
                    letterSpacing = CgTheme.type.pivot.letterSpacing,
                    lineHeight = CgTheme.type.valueLarge.fontSize * 1.1f,
                )
            } else {
                CgTheme.type.pivot
            },
    )
}

/** The question and the two ways in, as one grid of cells meeting on a hairline: stacked, or [sideBySide]. */
@Composable
private fun Choices(onRecord: () -> Unit, onEdit: () -> Unit, sideBySide: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CgSpace.S12)) {
        Text(
            stringResource(Res.string.home_start_question).caps(),
            style = CgTheme.type.labelStrong,
            color = CgColors.Text.Secondary,
        )
        val record: @Composable (Modifier) -> Unit = { m ->
            ChoiceCard(
                icon = CgIcons.Record,
                tileColor = CgColors.Status.Record,
                title = stringResource(Res.string.home_start_record_title),
                detail = stringResource(Res.string.home_start_record_detail),
                onClick = onRecord,
                signal = true,
                modifier = m.testTag(TestTags.StartRecord),
            )
        }
        val compose: @Composable (Modifier) -> Unit = { m ->
            ChoiceCard(
                icon = CgIcons.Film,
                tileColor = CgColors.Surface.Raised,
                title = stringResource(Res.string.home_start_edit_title),
                detail = stringResource(Res.string.home_start_edit_detail),
                onClick = onEdit,
                modifier = m.testTag(TestTags.StartCompose),
            )
        }
        if (sideBySide) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                record(Modifier.weight(1f).fillMaxHeight())
                compose(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column {
                record(Modifier)
                compose(Modifier)
            }
        }
    }
}

/**
 * What OnlyShots makes, drawn small: a shot's graph in a bar-coloured cell, which series it plots and how long it ran
 * over a grid, pressure in the signal, flow in bone, weight in blue. Decoration only.
 */
@Composable
private fun GraphCard(modifier: Modifier, big: Boolean) {
    Column(
        modifier
            .fillMaxWidth()
            .background(CgColors.Surface.Bar)
            .border(CgStroke.Hairline, CgColors.Line.Hairline)
            .padding(if (big) CgSpace.S16 else CgSpace.S12),
        verticalArrangement = Arrangement.spacedBy(if (big) CgSpace.S12 else CgSpace.S8),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                stringResource(Res.string.home_start_graph_series),
                style = CgTheme.type.data,
                color = CgColors.Text.Secondary,
            )
            Text("0:28", style = CgTheme.type.data, color = CgColors.Text.Secondary)
        }
        val trace = if (big) 2.dp else 1.5.dp
        // Built once per size, not on every draw.
        Spacer(
            Modifier.fillMaxWidth()
                .weight(1f)
                .clearAndSetSemantics {}
                .drawWithCache {
                    // The card draws it in a 300 × 120 box, stretched to fit.
                    val sx = size.width / 300f
                    val sy = size.height / 120f
                    val hair = CgStroke.Hairline.toPx()
                    val stroke = Stroke(trace.toPx())
                    val signal =
                        Path().apply {
                            moveTo(0f, 118f * sy)
                            cubicTo(20f * sx, 118f * sy, 30f * sx, 30f * sy, 50f * sx, 24f * sy)
                            lineTo(180f * sx, 24f * sy)
                            cubicTo(220f * sx, 26f * sy, 260f * sx, 40f * sy, 300f * sx, 48f * sy)
                        }
                    val primary =
                        Path().apply {
                            moveTo(0f, 118f * sy)
                            cubicTo(40f * sx, 116f * sy, 60f * sx, 90f * sy, 90f * sx, 72f * sy)
                            cubicTo(140f * sx, 60f * sy, 200f * sx, 56f * sy, 300f * sx, 54f * sy)
                        }
                    val blue =
                        Path().apply {
                            moveTo(0f, 118f * sy)
                            lineTo(40f * sx, 118f * sy)
                            cubicTo(120f * sx, 110f * sy, 200f * sx, 60f * sy, 300f * sx, 14f * sy)
                        }
                    onDrawBehind {
                        for (y in GRID_ROWS) {
                            drawLine(CgColors.Line.Divider, Offset(0f, y * sy), Offset(size.width, y * sy), hair)
                        }
                        for (x in GRID_COLUMNS) {
                            drawLine(CgColors.Line.Divider, Offset(x * sx, 0f), Offset(x * sx, size.height), hair)
                        }
                        drawPath(signal, CgColors.Brand.Signal, style = stroke)
                        drawPath(primary, CgColors.Text.Primary, style = stroke)
                        drawPath(blue, BluetoothBlue, style = stroke)
                    }
                }
        )
    }
}

/** The card's grid, in its 300 × 120 box. */
private val GRID_ROWS = floatArrayOf(24f, 60f, 96f)
private val GRID_COLUMNS = floatArrayOf(75f, 150f, 225f)

/** The phone card: its header and a 112 dp graph, as the canvas draws it. */
private val GRAPH_CARD_PHONE = 152.dp

/** The instrument's faint ground: a 24 dp grid of bone lines at 4 %. */
private fun DrawScope.drawGrid() {
    val step = 24.dp.toPx()
    val line = CgColors.Text.Primary.copy(alpha = 0.04f)
    var x = 0f
    while (x < size.width) {
        drawLine(line, Offset(x, 0f), Offset(x, size.height), CgStroke.Hairline.toPx())
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(line, Offset(0f, y), Offset(size.width, y), CgStroke.Hairline.toPx())
        y += step
    }
}
