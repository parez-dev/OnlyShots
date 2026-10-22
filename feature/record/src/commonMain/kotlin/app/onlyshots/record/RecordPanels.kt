package app.onlyshots.record

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.onlyshots.TestTags
import app.onlyshots.camera.CameraState
import app.onlyshots.core.designsystem.CgColors
import app.onlyshots.core.designsystem.CgIcons
import app.onlyshots.core.designsystem.CgMotion
import app.onlyshots.core.designsystem.CgMotion.tick
import app.onlyshots.core.designsystem.CgSize
import app.onlyshots.core.designsystem.CgSpace
import app.onlyshots.core.designsystem.CgStroke
import app.onlyshots.core.designsystem.CgTheme
import app.onlyshots.core.designsystem.caps
import app.onlyshots.core.designsystem.component.BackButton
import app.onlyshots.core.designsystem.component.BarIcon
import app.onlyshots.core.designsystem.component.CgIcon
import app.onlyshots.core.designsystem.component.HairlineGrid
import app.onlyshots.core.designsystem.component.IconCircleButton
import app.onlyshots.core.designsystem.component.LocalNativeChrome
import app.onlyshots.core.designsystem.component.NativeBarItem
import app.onlyshots.core.designsystem.component.NativeItems
import app.onlyshots.core.designsystem.component.Pill
import app.onlyshots.core.designsystem.component.ReadingTile
import app.onlyshots.core.designsystem.component.RecordButton
import app.onlyshots.core.designsystem.component.RecordButtonState
import app.onlyshots.core.designsystem.component.RuleStatusRow
import app.onlyshots.core.designsystem.component.RulesStripHint
import app.onlyshots.core.designsystem.component.SourceChip
import app.onlyshots.core.designsystem.component.StatusChip
import app.onlyshots.core.model.RecordingSettings
import app.onlyshots.core.model.Sample
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.UiText
import app.onlyshots.core.resources.record_armed
import app.onlyshots.core.resources.record_no_data_hint
import app.onlyshots.core.resources.record_ready
import app.onlyshots.core.resources.record_rule_start
import app.onlyshots.core.resources.record_rule_stop
import app.onlyshots.core.resources.record_settings
import app.onlyshots.core.resources.record_simulate_shot
import app.onlyshots.core.resources.record_status_no_machine
import app.onlyshots.core.resources.record_stops
import app.onlyshots.core.resources.text
import app.onlyshots.layout.LayoutKind
import app.onlyshots.layout.LocalWindowLayout
import app.onlyshots.machineLabel
import org.jetbrains.compose.resources.stringResource

/** What the panels around the camera share, so each posture's panel is a few lines of layout. */
internal class TakeInputs(
    val state: RecordUiState,
    val camera: CameraState,
    val settings: RecordingSettings,
    /** The source's newest reading, read only where readings are shown: it changes with every sample. */
    val live: () -> Sample?,
    val actions: RecordActions,
    val canSimulateShot: Boolean,
    val saveError: UiText?,
    val onBack: () -> Unit,
    val onSettings: () -> Unit,
) {
    /** "simulate shot" can pull the replayed shot: armed, with the camera. */
    val canSimulate: Boolean
        get() = canSimulateShot && state.phase == RecordPhase.ARMED && !state.noCamera

    /** The newest reading: the shot's while recording, the source's before. */
    val sample: Sample?
        get() = if (state.recording) state.shot.samples.lastOrNull() ?: live() else live()
}

/** Back (where the panel has it), the source with its live dot, and the recording settings on the right. */
@Composable
internal fun TakeTopRow(take: TakeInputs, back: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CgSpace.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mid-take there is no leaving: the key stays, inert, until the take is saved.
        if (back) BackButton(onClick = take.onBack, enabled = !take.state.recording)
        val source = machineLabel(take.state.sourceKind)
        SourceChip(
            if (take.state.hasData) source else stringResource(Res.string.record_status_no_machine),
            live = take.state.hasData,
        )
        Spacer(Modifier.weight(1f))
        // Start, pre-roll and stop rules, as the phone's gear: they need a machine and a camera, and are closed
        // mid-shot. Where the bars are native (iPhone Duo) the gear is their item.
        val settings = take.state.hasData && !take.state.recording && !take.state.noCamera
        val native = LocalNativeChrome.current != null
        val settingsTitle = stringResource(Res.string.record_settings)
        NativeItems(
            if (settings) listOf(NativeBarItem(TestTags.RecordSettings, settingsTitle, BarIcon.Settings))
            else emptyList()
        ) {
            take.onSettings()
        }
        KeyIn(settings && !native) {
            IconCircleButton(
                CgIcons.Settings,
                stringResource(Res.string.record_settings),
                onClick = take.onSettings,
                modifier = Modifier.testTag(TestTags.RecordSettings),
            )
        }
    }
}

/** The readings as tiles, [columns] to a row, coloured as the graph draws them. */
@Composable
internal fun ReadingsGrid(take: TakeInputs, columns: Int, large: Boolean, only: Set<Quantity>? = null) {
    val readings = takeReadings(take.sample, take.state.sourceKind).filter { only == null || it.quantity in only }
    // One grid: the cells meet on single hairlines.
    HairlineGrid(readings.size, columns) { i, cell ->
        val r = readings[i]
        ReadingTile(r.label.text(), r.value, stringResource(r.unit), cell, r.quantity.color(), large, framed = false)
    }
}

/** How the take starts and stops: done or waiting while it runs, the rules themselves before. */
@Composable
internal fun RulesBlock(take: TakeInputs, modifier: Modifier = Modifier) {
    val settings = take.settings
    val stop = RecordSession.stopRuleShort(settings.effectiveFor(take.state.sourceKind).stopRule).text()
    // Without a camera there is no take to rule: the bar holds only the key, greyed.
    if (take.state.noCamera && !take.state.recording) return
    val mode =
        when {
            take.state.recording -> RulesMode.RUNNING
            take.state.hasData -> RulesMode.RULES
            else -> RulesMode.HINT
        }
    // The rules turn into their progress when recording starts: the block ticks over, like a readout, to its new
    // height.
    AnimatedContent(
        mode,
        transitionSpec = { tick() },
        label = "rules",
        modifier = modifier,
    ) { shown ->
        RulesBlockContent(take, shown, stop)
    }
}

private enum class RulesMode {
    RUNNING,
    RULES,
    HINT,
}

@Composable
private fun RulesBlockContent(take: TakeInputs, mode: RulesMode, stop: String) {
    val settings = take.settings
    if (mode == RulesMode.RUNNING) {
        Column(Modifier.fillMaxWidth().background(CgColors.Surface.Base)) {
            RuleStatusRow(startRuleLine(settings).text(), done = take.state.shot.samples.isNotEmpty())
            Box(Modifier.fillMaxWidth().height(CgStroke.Hairline).background(CgColors.Line.Divider))
            RuleStatusRow(stringResource(Res.string.record_stops, stop), done = false)
        }
    } else if (mode == RulesMode.RULES) {
        val (start, startDetail) = RecordSession.startRuleShort(settings)
        RulesGrid(start.text() + startDetail?.text()?.let { " · $it" }.orEmpty(), stop)
    } else {
        RulesStripHint(stringResource(Res.string.record_no_data_hint), Modifier.background(CgColors.Surface.Base))
    }
}

/** How the take starts and stops, as two cells side by side with a hairline between: ▶ the start, ■ the stop. */
@Composable
private fun RulesGrid(start: String, stop: String) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(CgColors.Surface.Base).semantics(
            mergeDescendants = true
        ) {}
    ) {
        RuleCell(CgIcons.Play, stringResource(Res.string.record_rule_start), start, Modifier.weight(1f))
        Box(Modifier.width(CgStroke.Hairline).fillMaxHeight().background(CgColors.Line.Hairline))
        RuleCell(CgIcons.Stop, stringResource(Res.string.record_rule_stop), stop, Modifier.weight(1f))
    }
}

/** One rule's cell: what it is in grey (▶ START), and under it the rule itself. */
@Composable
private fun RuleCell(icon: ImageVector, label: String, text: String, modifier: Modifier) {
    Column(
        modifier.heightIn(min = CgSize.Touch).padding(horizontal = CgSpace.S16, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(CgSpace.S4),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(CgSpace.S6), verticalAlignment = Alignment.CenterVertically) {
            CgIcon(icon, tint = CgColors.Text.Secondary, size = 10.dp)
            Text(label.caps(), style = CgTheme.type.data, color = CgColors.Text.Secondary)
        }
        Text(text.caps(), style = CgTheme.type.labelStrong.copy(letterSpacing = 0.sp))
    }
}

/**
 * Where the take stands, over the picture: armed, a signal-edged notice (it starts by itself); otherwise the status
 * chip.
 */
@Composable
internal fun TakeStatus(state: RecordUiState, modifier: Modifier = Modifier) {
    val text = state.status.text()
    if (state.phase == RecordPhase.ARMED && !state.recording && !state.noCamera) {
        Text(
            text.caps(),
            style = CgTheme.type.labelStrong.copy(letterSpacing = 0.sp),
            color = CgColors.Brand.Signal,
            modifier =
                modifier
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .background(CgColors.Surface.Base.copy(alpha = 0.85f))
                    .border(CgStroke.Hairline, CgColors.Line.Hairline)
                    .padding(horizontal = 10.dp, vertical = CgSpace.S6),
        )
    } else {
        StatusChip(
            text = text,
            recording = state.recording,
            dot = if (state.recording) CgColors.Text.Primary else null,
            modifier = modifier,
        )
    }
}

/**
 * The clock and the quality on the left, the record button on the right and "simulate shot" beside it, where there is
 * one. [clock] is how large the clock is: as large as the posture's panel has room for. Armed, the word is the signal.
 */
@Composable
internal fun TakeFooter(take: TakeInputs, clock: TextStyle = CgTheme.type.cardTitle) {
    val state = take.state
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CgSpace.S16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CgSpace.S4)) {
            // While filming, the take's one clock (the brew's, once the shot runs); before, a word says where
            // things stand. One ticks over into the other.
            AnimatedContent(
                state.recording,
                transitionSpec = { tick() },
                label = "clock",
            ) { recording ->
                if (recording) Text(state.status.text(), style = clock)
                else {
                    val armed = state.phase == RecordPhase.ARMED
                    Text(
                        stringResource(if (armed) Res.string.record_armed else Res.string.record_ready).caps(),
                        style = clock,
                        color = if (armed) CgColors.Brand.Signal else CgColors.Text.Primary,
                    )
                }
            }
            Text(
                takeQuality(take.camera.supports4k, take.camera.micOn).text(),
                style = CgTheme.type.caption,
                color = CgColors.Text.Secondary,
            )
        }
        RecordButton(
            state.buttonState(),
            onClick = take.actions::recordPressed,
            modifier = Modifier.testTag(TestTags.RecordButton),
        )
        AnimatedVisibility(
            take.canSimulate,
            enter = expandHorizontally(CgMotion.sizeSpec, expandFrom = Alignment.Start),
            exit = shrinkHorizontally(CgMotion.sizeSpec, shrinkTowards = Alignment.Start),
        ) {
            Pill(
                stringResource(Res.string.record_simulate_shot),
                onClick = take.actions::simulateShot,
                modifier = Modifier.widthIn(max = SIMULATE_WIDTH).testTag(TestTags.RecordSimulate),
            )
        }
    }
}

/**
 * The panel's column: what scrolls on top, and [TakeFooter] pinned at the bottom, so the record button stays where the
 * thumb expects it.
 */
@Composable
internal fun PanelColumn(
    modifier: Modifier = Modifier,
    footer: TakeInputs? = null,
    clock: TextStyle = CgTheme.type.cardTitle,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().padding(CgSpace.S16, CgSpace.S16, CgSpace.S16, CgSpace.S24)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content,
        )
        footer?.let {
            // Under a hairline, the clock and the button: where the thumb expects them.
            Spacer(Modifier.size(CgSpace.S12))
            Box(Modifier.fillMaxWidth().height(CgStroke.Hairline).background(CgColors.Line.Hairline))
            Spacer(Modifier.size(CgSpace.S12))
            TakeFooter(it, clock)
        }
    }
}

/**
 * A book foldable resting on its side: under the camera, the source along the top; then the status, the rules and the
 * readings that matter before a shot on the left, and beside them, past a hairline, the lenses, the clock and the
 * button. [zoom] is the lenses' strip, where the camera has more than one.
 */
@Composable
internal fun TabletopPanel(
    take: TakeInputs,
    modifier: Modifier = Modifier,
    zoom: @Composable (Modifier) -> Unit = {},
) {
    Column(
        modifier.fillMaxSize().padding(start = CgSpace.S24, end = CgSpace.S24, top = CgSpace.S12, bottom = CgSpace.S16),
        verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
    ) {
        TakeTopRow(take, back = true)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CgSpace.S24)) {
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
            ) {
                if (take.state.statusWorthShowing) TakeStatus(take.state)
                RulesBlock(take, Modifier.border(CgStroke.Hairline, CgColors.Line.Hairline))
                // Recording, every reading; before, the two that matter, one ticking over into the other.
                AnimatedContent(
                    take.state.recording,
                    transitionSpec = { tick() },
                    label = "tabletop",
                ) { recording ->
                    if (recording) ReadingsGrid(take, columns = 4, large = false)
                    else
                        ReadingsGrid(
                            take,
                            columns = 2,
                            large = true,
                            only = setOf(Quantity.Temperature, Quantity.Weight),
                        )
                }
                Banners(take.camera, take.saveError)
            }
            Column(
                Modifier.width(TABLETOP_SIDE)
                    .fillMaxHeight()
                    .drawBehind { drawRect(CgColors.Line.Hairline, size = Size(CgStroke.Hairline.toPx(), size.height)) }
                    .padding(start = CgSpace.S24),
                verticalArrangement = Arrangement.spacedBy(CgSpace.S16, Alignment.Bottom),
            ) {
                zoom(Modifier)
                TakeFooter(take, clock = CgTheme.type.pivot)
            }
        }
    }
}

/**
 * A flip in flex mode: the camera stands in the upper half, and the lower half is the controls. Back and the source,
 * the readings as large cells, the rules, and under a hairline the clock, large, with the button.
 */
@Composable
internal fun FlexPanel(take: TakeInputs, modifier: Modifier = Modifier) {
    PanelColumn(modifier, footer = take, clock = CgTheme.type.valueLarge) {
        TakeTopRow(take, back = true)
        ReadingsGrid(take, columns = 2, large = true)
        RulesBlock(take, Modifier.border(CgStroke.Hairline, CgColors.Line.Hairline))
        Banners(take.camera, take.saveError)
    }
}

/** The colour the graph draws this quantity in. */
private fun Quantity.color(): Color =
    when (this) {
        Quantity.Pressure -> CgColors.Series.Pressure
        Quantity.Flow -> CgColors.Series.Flow
        Quantity.Weight -> CgColors.Text.Primary
        Quantity.WeightFlow -> CgColors.Series.WeightFlow
        Quantity.Temperature -> CgColors.Series.Temperature
    }

/**
 * Whether the status chip says anything the screen does not already: without a machine it would only say so again,
 * beside the source's chip and the hint over the button.
 */
internal val RecordUiState.statusWorthShowing: Boolean
    get() = hasData || recording || noCamera

/** The record button's look for the session's phase. */
internal fun RecordUiState.buttonState(): RecordButtonState =
    when {
        recording -> RecordButtonState.Recording
        noCamera -> RecordButtonState.Disabled
        phase == RecordPhase.ARMED -> RecordButtonState.Armed
        else -> RecordButtonState.Idle
    }

/**
 * The full-screen camera's controls, floating over the picture: back, the source and the settings along the top, the
 * status under them, the rules and the record key at the bottom. Where they go follows the window
 * ([LocalWindowLayout]):
 * - a phone held upright: nothing across the picture but a shade at its foot; the lenses over the record key (and
 *   "simulate shot" to its right) float there. The status and the rules float as cards out of the graph's way: under
 *   the top row while the graph is low, over the lenses while it is high;
 * - a tablet held upright: the lenses, the key and the rules float at the bottom, centred, the rules as a card;
 * - on its side: the lenses, the key and "simulate shot" down the trailing edge, the rules at the bottom on the start;
 * - an open book foldable: never across the fold. The top row, the status and the rules on the left page; the lenses
 *   and the key at the foot of the right page, where the graph is kept.
 *
 * [zoom] is the lenses' strip, standing (`vertical`) on its side. [onClear] gets what of the window, in root px, the
 * controls leave free: the graph is kept there.
 */
@Composable
internal fun FloatingControls(
    take: TakeInputs,
    landscape: Boolean,
    /** The graph is in the upper half: the status chip goes over the bottom controls, out of its way. */
    graphOnTop: Boolean,
    zoom: @Composable (Modifier, vertical: Boolean) -> Unit,
    onClear: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layout = LocalWindowLayout.current
    val fold = layout.fold && layout.kind == LayoutKind.Wide
    val tablet = layout.kind == LayoutKind.Wide
    val state = take.state
    // Where the controls are, in root px, kept out of snapshot state: they move on every layout pass (a rules block
    // resizing), and only what they leave free is passed on, when it changes.
    val rects = remember { ControlRects() }
    val clearChanged by rememberUpdatedState(onClear)
    val landscapeNow by rememberUpdatedState(landscape)
    val foldNow by rememberUpdatedState(fold)
    val report = remember {
        {
            val free = rects.free(landscapeNow, foldNow)
            if (free != null && free != rects.reported) {
                rects.reported = free
                clearChanged(free)
            }
        }
    }
    LaunchedEffect(landscape, fold) { report() }
    val status = @Composable { m: Modifier -> if (state.statusWorthShowing) TakeStatus(state, m) }
    // The take's time is the status chip's, one clock: here only "simulate shot", where there is one.
    val simulate =
        @Composable {
            if (!state.recording && take.canSimulate) {
                Pill(
                    stringResource(Res.string.record_simulate_shot),
                    onClick = take.actions::simulateShot,
                    modifier = Modifier.widthIn(max = SIMULATE_WIDTH).testTag(TestTags.RecordSimulate),
                )
            }
        }
    val recordKey =
        @Composable {
            RecordButton(
                state.buttonState(),
                onClick = take.actions::recordPressed,
                modifier = Modifier.testTag(TestTags.RecordButton),
            )
        }
    // The key centred, "simulate shot" to its right.
    val keyRow =
        @Composable { m: Modifier ->
            Row(m, verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                recordKey()
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { simulate() }
            }
        }
    val rulesCard = @Composable { m: Modifier -> RulesBlock(take, m.border(CgStroke.Hairline, CgColors.Line.Hairline)) }
    val upright = !fold && !landscape && !tablet
    Box(
        modifier.fillMaxSize().onGloballyPositioned {
            rects.window = it.boundsInRoot()
            report()
        }
    ) {
        if (fold) {
            // Two pages meeting at the fold, measured from the window's edge as the layout is.
            Box(Modifier.fillMaxHeight().width(layout.splitDp.dp)) {
                Column(
                    Modifier.fillMaxSize().safeDrawingPadding().padding(CgSpace.S16),
                    verticalArrangement = Arrangement.spacedBy(CgSpace.S8),
                ) {
                    Box(
                        Modifier.onGloballyPositioned {
                            rects.top = it.boundsInRoot()
                            report()
                        }
                    ) {
                        TakeTopRow(take, back = true)
                    }
                    Banners(take.camera, take.saveError)
                    status(Modifier)
                    Spacer(Modifier.weight(1f))
                    rulesCard(Modifier.fillMaxWidth())
                }
            }
            Box(
                Modifier.fillMaxSize().padding(start = (layout.splitDp + layout.hingeDp).dp).onGloballyPositioned {
                    rects.page = it.boundsInRoot()
                    report()
                }
            ) {
                Column(
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .padding(CgSpace.S16)
                        .onGloballyPositioned {
                            rects.bottom = it.boundsInRoot()
                            report()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                ) {
                    zoom(Modifier, false)
                    keyRow(Modifier.fillMaxWidth())
                }
            }
            return@Box
        }
        if (landscape)
            Box(
                Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(SIDE_SHADE)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, SHADE)))
            )
        if (upright)
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(FOOT_SHADE)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, SHADE)))
            )
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(if (tablet) CgSpace.S24 else CgSpace.S12)) {
            Column(
                Modifier.align(Alignment.TopStart).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CgSpace.S8),
                // On its side the status stands at the trailing edge, over the key.
                horizontalAlignment = if (landscape) Alignment.End else Alignment.Start,
            ) {
                Box(
                    Modifier.onGloballyPositioned {
                        rects.top = it.boundsInRoot()
                        report()
                    }
                ) {
                    TakeTopRow(take, back = true)
                }
                Banners(take.camera, take.saveError)
                if (!graphOnTop || landscape) status(Modifier)
                if (upright && !graphOnTop) rulesCard(Modifier.fillMaxWidth())
            }
            if (landscape) {
                // Down the trailing edge: the lenses standing over the key, "simulate shot" under it.
                Column(
                    Modifier.align(Alignment.CenterEnd).onGloballyPositioned {
                        rects.side = it.boundsInRoot()
                        report()
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                ) {
                    zoom(Modifier, true)
                    recordKey()
                    simulate()
                }
                Column(
                    Modifier.align(Alignment.BottomStart).widthIn(max = RULES_WIDTH).onGloballyPositioned {
                        rects.bottom = it.boundsInRoot()
                        report()
                    },
                    verticalArrangement = Arrangement.spacedBy(CgSpace.S8),
                ) {
                    rulesCard(Modifier)
                }
            } else if (tablet) {
                // A tablet held upright: the lenses, the key and the rules float at the foot of the picture.
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().onGloballyPositioned {
                        rects.bottom = it.boundsInRoot()
                        report()
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                ) {
                    if (graphOnTop) status(Modifier.align(Alignment.Start))
                    zoom(Modifier, false)
                    keyRow(Modifier.widthIn(max = RULES_WIDTH).fillMaxWidth())
                    rulesCard(Modifier.widthIn(max = RULES_WIDTH))
                }
            } else {
                // Held upright: the lenses over the key at the foot, and over them, while the graph is high, the
                // status and the rules.
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CgSpace.S12),
                ) {
                    if (graphOnTop) {
                        status(Modifier)
                        rulesCard(Modifier.fillMaxWidth())
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(top = CgSpace.S4).onGloballyPositioned {
                            rects.bottom = it.boundsInRoot()
                            report()
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(CgSpace.S16),
                    ) {
                        zoom(Modifier, false)
                        keyRow(Modifier.fillMaxWidth().padding(bottom = CgSpace.S8))
                    }
                }
            }
        }
    }
}

/** The shade behind the record key on its side, so it reads over a bright picture. */
private val SHADE = CgColors.Surface.Scrim.copy(alpha = 0.55f)
private val SIDE_SHADE = 200.dp
/** Upright, the shade under the lenses and the key. */
private val FOOT_SHADE = 240.dp

/** "simulate shot" at most: two short lines beside the record key. */
private val SIMULATE_WIDTH = 120.dp

/** The tabletop panel's trailing column: the lenses, the clock and the key. */
private val TABLETOP_SIDE = 400.dp

/** The rules strip at most: on a tablet it does not run across the whole picture. */
private val RULES_WIDTH = 480.dp

/** Where the floating controls are, in root px, and what of the window they last left free. */
private class ControlRects {
    var top = Rect.Zero
    var bottom = Rect.Zero
    var side = Rect.Zero
    var window = Rect.Zero
    /** The right page on a fold: the graph is kept on that page. */
    var page = Rect.Zero
    var reported: Rect? = null

    fun free(landscape: Boolean, fold: Boolean): Rect? =
        when {
            window.isEmpty -> null
            fold && !page.isEmpty ->
                Rect(page.left, page.top, page.right, if (bottom.isEmpty) page.bottom else bottom.top)
            else ->
                Rect(
                    window.left,
                    top.bottom.coerceAtLeast(window.top),
                    if (landscape && !side.isEmpty) side.left else window.right,
                    if (bottom.isEmpty) window.bottom else bottom.top,
                )
        }
}
