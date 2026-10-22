package app.onlyshots.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.onlyshots.PRIVACY_URL
import app.onlyshots.StopRuleChoice
import app.onlyshots.TestTags
import app.onlyshots.core.designsystem.CgColors
import app.onlyshots.core.designsystem.CgIcons
import app.onlyshots.core.designsystem.CgMotion
import app.onlyshots.core.designsystem.CgSize
import app.onlyshots.core.designsystem.CgSpace
import app.onlyshots.core.designsystem.CgStroke
import app.onlyshots.core.designsystem.CgTheme
import app.onlyshots.core.designsystem.caps
import app.onlyshots.core.designsystem.component.AddRow
import app.onlyshots.core.designsystem.component.Arriving
import app.onlyshots.core.designsystem.component.BarIcon
import app.onlyshots.core.designsystem.component.BarPriority
import app.onlyshots.core.designsystem.component.BluetoothBlue
import app.onlyshots.core.designsystem.component.CgButton
import app.onlyshots.core.designsystem.component.CgButtonKind
import app.onlyshots.core.designsystem.component.CgIcon
import app.onlyshots.core.designsystem.component.CgSheet
import app.onlyshots.core.designsystem.component.CgStepper
import app.onlyshots.core.designsystem.component.CgTextField
import app.onlyshots.core.designsystem.component.ConnectScaleCard
import app.onlyshots.core.designsystem.component.HairlineGrid
import app.onlyshots.core.designsystem.component.HairlineStack
import app.onlyshots.core.designsystem.component.HeaderBack
import app.onlyshots.core.designsystem.component.MachineRow
import app.onlyshots.core.designsystem.component.NativeBarItem
import app.onlyshots.core.designsystem.component.NativeItems
import app.onlyshots.core.designsystem.component.ReadingTile
import app.onlyshots.core.designsystem.component.RecordAction
import app.onlyshots.core.designsystem.component.RuleRow
import app.onlyshots.core.designsystem.component.ScaleAction
import app.onlyshots.core.designsystem.component.ScaleDeviceRow
import app.onlyshots.core.designsystem.component.ScaleSkeletonBars
import app.onlyshots.core.designsystem.component.SectionHeader
import app.onlyshots.core.designsystem.component.SegmentedControl
import app.onlyshots.core.designsystem.component.SettingRow
import app.onlyshots.core.designsystem.component.SheetHeader
import app.onlyshots.core.designsystem.component.SkeletonRow
import app.onlyshots.core.designsystem.component.rememberListSettled
import app.onlyshots.core.designsystem.component.topRule
import app.onlyshots.core.model.AppVersion
import app.onlyshots.core.model.RecordingSettings
import app.onlyshots.core.model.SourceKind
import app.onlyshots.core.model.StopRule
import app.onlyshots.core.model.availableFor
import app.onlyshots.core.model.stepLabel
import app.onlyshots.core.model.stepped
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.core_record
import app.onlyshots.core.resources.core_version
import app.onlyshots.core.resources.home_about_text
import app.onlyshots.core.resources.home_add_by_address
import app.onlyshots.core.resources.home_add_by_address_subtitle
import app.onlyshots.core.resources.home_address_label
import app.onlyshots.core.resources.home_address_note
import app.onlyshots.core.resources.home_bluetooth_off
import app.onlyshots.core.resources.home_bluetooth_scale
import app.onlyshots.core.resources.home_connect
import app.onlyshots.core.resources.home_connect_scale_subtitle
import app.onlyshots.core.resources.home_connect_scale_title
import app.onlyshots.core.resources.home_help_machines
import app.onlyshots.core.resources.home_help_machines_note
import app.onlyshots.core.resources.home_help_scales_note
import app.onlyshots.core.resources.home_machines_title
import app.onlyshots.core.resources.home_next_take
import app.onlyshots.core.resources.home_privacy
import app.onlyshots.core.resources.home_record
import app.onlyshots.core.resources.home_record_armed_auto
import app.onlyshots.core.resources.home_record_armed_manual
import app.onlyshots.core.resources.home_rule_pre_roll
import app.onlyshots.core.resources.home_rule_start
import app.onlyshots.core.resources.home_rule_stop
import app.onlyshots.core.resources.home_scale_detail
import app.onlyshots.core.resources.home_scales_title
import app.onlyshots.core.resources.home_take_camera_only
import app.onlyshots.core.resources.home_take_machine
import app.onlyshots.core.resources.home_title
import app.onlyshots.core.resources.home_try_sample
import app.onlyshots.core.resources.home_turn_on_bluetooth
import app.onlyshots.core.resources.open_dismiss
import app.onlyshots.core.resources.record_pre_roll_hint
import app.onlyshots.core.resources.record_pre_roll_hint_scale
import app.onlyshots.core.resources.record_seconds
import app.onlyshots.core.resources.source_scale
import app.onlyshots.core.resources.text
import app.onlyshots.edit.machineColor
import app.onlyshots.layout.LayoutKind
import app.onlyshots.layout.LocalWindowLayout
import app.onlyshots.layout.WidePanes
import app.onlyshots.platform.rememberBluetoothPermission
import app.onlyshots.sources.ConnectionState
import app.onlyshots.sources.ble.FoundBleDevice
import app.onlyshots.sources.machine.FoundMachine
import app.onlyshots.sources.machine.NetworkMachines
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onTrySample: () -> Unit = onOpenCamera,
    onBack: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The next take changes with every live sample: handed down as a provider, read only where it is shown.
    val take = viewModel.take.collectAsStateWithLifecycle()
    val askBluetooth = rememberBluetoothPermission(viewModel::onBluetoothAnswer)
    // Scales are searched for all the time, so Bluetooth is asked for as Home first opens; already allowed, this
    // answers at once without a prompt. Refused, the "connect a scale" card asks again.
    LaunchedEffect(Unit) { if (!viewModel.askedForBluetooth) askBluetooth() }
    LifecycleStartEffect(viewModel) {
        viewModel.endSample()
        viewModel.startSearching()
        onStopOrDispose { viewModel.stopSearching() }
    }
    HomeBody(
        state = state,
        onMachine = viewModel::connectTo,
        onScale = viewModel::connectScale,
        onAddByAddress = viewModel::openAddress,
        onAllowBluetooth = askBluetooth,
        // Recording without data is just that: whatever the sample left behind is put back first.
        onOpenCamera = {
            viewModel.endSample()
            onOpenCamera()
        },
        // The sample needs nothing to be picked: the camera opens and the shot plays as soon as it is armed.
        onSample = {
            viewModel.useSample()
            onTrySample()
        },
        onBack = onBack,
        takeOf = { take.value },
        onRecording = viewModel::updateRecording,
        foldPage =
            if (state.addressSheet && LocalWindowLayout.current.fold) {
                {
                    AddressPage(
                        address = state.address,
                        onAddress = viewModel::setAddress,
                        onConnect = viewModel::addByAddress,
                        onDismiss = viewModel::closeAddress,
                    )
                }
            } else {
                null
            },
        onFoldPageDismiss = viewModel::closeAddress,
    )
    if (state.addressSheet && !LocalWindowLayout.current.fold) {
        AddressSheet(
            address = state.address,
            onAddress = viewModel::setAddress,
            onConnect = viewModel::addByAddress,
            onDismiss = viewModel::closeAddress,
        )
    }
}

/** Figma "Home" without the view model, so every state can be drawn from a plain [HomeUiState]. */
@Composable
fun HomeBody(
    state: HomeUiState,
    onMachine: (FoundMachine) -> Unit,
    onScale: (FoundBleDevice) -> Unit,
    onAddByAddress: () -> Unit,
    onAllowBluetooth: () -> Unit,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier,
    take: NextTake = NextTake(),
    onRecording: ((RecordingSettings) -> RecordingSettings) -> Unit = {},
    onSample: () -> Unit = {},
    onBack: () -> Unit = {},
    /**
     * On a book foldable, what a phone shows in a sheet (adding a machine by its address) takes the right page, in
     * place of the next take, and the left one dims; [onFoldPageDismiss] closes it from there.
     */
    foldPage: (@Composable () -> Unit)? = null,
    onFoldPageDismiss: () -> Unit = {},
    /** The next take as a provider, in place of [take]: the live screen's, read only where the readings are drawn. */
    takeOf: (() -> NextTake)? = null,
) {
    val nextTake = takeOf ?: { take }
    val layout = LocalWindowLayout.current
    HomeNativeBar(recordAction(state).enabled, onOpenCamera, onAddByAddress)
    if (layout.kind == LayoutKind.Wide) {
        // Two pages: where the shot comes from, and the take the record button starts.
        Box(modifier.fillMaxSize().background(CgColors.Surface.Base)) {
            WidePanes(
                layout,
                Modifier.fillMaxSize().safeDrawingPadding(),
                main = {
                    Box(Modifier.fillMaxSize()) {
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            // Room for both lists side by side (a tablet on its side): machines and scales as two
                            // columns; otherwise one under the other.
                            val columns = !layout.fold && maxWidth >= TWO_COLUMNS
                            val side = if (layout.fold) CgSpace.S16 else 32.dp
                            Column(Modifier.fillMaxSize().padding(horizontal = side - CgSpace.S16)) {
                                Header(onBack)
                                if (columns) {
                                    Row(
                                        Modifier.weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = CgSpace.S16)
                                            .padding(top = CgSpace.S16, bottom = CgSpace.S24),
                                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                                    ) {
                                        Column(
                                            Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(CgSpace.S8),
                                        ) {
                                            Machines(state, onMachine, onAddByAddress)
                                        }
                                        Column(
                                            Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(CgSpace.S8),
                                        ) {
                                            Scales(state, onScale, onAllowBluetooth)
                                        }
                                    }
                                } else {
                                    Column(
                                        Modifier.weight(1f)
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = CgSpace.S16)
                                            .padding(top = CgSpace.S6, bottom = CgSpace.S24),
                                        verticalArrangement = Arrangement.spacedBy(CgSpace.S8),
                                    ) {
                                        Machines(state, onMachine, onAddByAddress)
                                        Scales(state, onScale, onAllowBluetooth)
                                    }
                                }
                            }
                        }
                        if (foldPage != null) {
                            Box(
                                Modifier.fillMaxSize()
                                    .background(CgColors.Surface.Scrim.copy(alpha = 0.6f))
                                    // The dimmed page closes the other one: said so, and no ripple over a whole page.
                                    .clickable(
                                        remember { MutableInteractionSource() },
                                        indication = null,
                                        onClickLabel = stringResource(Res.string.open_dismiss),
                                        onClick = onFoldPageDismiss,
                                    )
                            )
                        }
                    }
                },
                panel = {
                    if (foldPage != null) foldPage()
                    else NextTakePane(state, nextTake, onRecording, onOpenCamera, onSample)
                },
            )
        }
        return
    }
    Box(modifier.fillMaxSize().background(CgColors.Surface.Base).safeDrawingPadding(), Alignment.TopCenter) {
        // The design is a phone column; on a tablet it stays one, centred.
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
            Header(onBack)
            Column(
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = CgSpace.S16, end = CgSpace.S16, top = CgSpace.S6, bottom = CgSpace.S16),
                verticalArrangement = Arrangement.spacedBy(CgSpace.S8),
            ) {
                Machines(state, onMachine, onAddByAddress)
                Scales(state, onScale, onAllowBluetooth)
            }
            val action = recordAction(state)
            Column(
                Modifier.padding(start = CgSpace.S16, end = CgSpace.S16, top = CgSpace.S12, bottom = CgSpace.S8),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RecordAction(
                    text = action.text.text(),
                    caption = action.caption.text(),
                    enabled = action.enabled,
                    onClick = onOpenCamera,
                    modifier = Modifier.testTag(TestTags.HomeRecordAction),
                    withData = action.withData,
                )
                SampleLink(onSample)
            }
        }
    }
}

@Composable
/** The same header as the phones' "compose a video": back to Start and the flow's name. Help is on Start. */
private fun Header(onBack: () -> Unit) {
    HeaderBack(
        stringResource(Res.string.home_title),
        onBack = onBack,
        modifier = Modifier.padding(vertical = CgSpace.S8),
    )
}

@Composable
private fun Machines(state: HomeUiState, onMachine: (FoundMachine) -> Unit, onAddByAddress: () -> Unit) {
    val rows = machineRows(state)
    SectionHeader(
        stringResource(Res.string.home_machines_title),
        trailing = foundNote(state.machines.size).text(),
        trailingColor = if (state.machines.isEmpty()) CgColors.Text.Secondary else CgColors.Text.Primary,
        // The search goes on after a find (there may be more than one machine): the light stays on beside the count.
        searching = true,
    )
    // Machines turn up one by one as the search finds them: each slides open rather than pushing the list down. They
    // stack as one grid, meeting on single hairlines.
    val settled = rememberListSettled()
    if (rows.isNotEmpty()) {
        HairlineStack {
            for (row in rows) {
                key(row.kind, row.selected, row.found?.host ?: row.detail) {
                    Arriving(settled) {
                        MachineRow(
                            name = row.name.text(),
                            detail = row.detail,
                            icon = CgIcons.Machine,
                            status = stringResource(row.status.label),
                            statusDot = dotOf(row.status),
                            selected = row.selected,
                            onClick = { row.found?.let(onMachine) },
                            enabled = row.found != null || row.selected,
                            offline = row.status == RowStatus.Offline,
                            framed = false,
                        )
                    }
                }
            }
        }
    }
    if (state.machines.isEmpty()) SkeletonRow()
    AddRow(
        stringResource(Res.string.home_add_by_address),
        onClick = onAddByAddress,
        modifier = Modifier.testTag(TestTags.HomeAddByAddress),
        subtitle = if (rows.isEmpty()) stringResource(Res.string.home_add_by_address_subtitle) else null,
    )
}

@Composable
private fun Scales(state: HomeUiState, onScale: (FoundBleDevice) -> Unit, onAllowBluetooth: () -> Unit) {
    val scale = state.savedScale
    val others = otherScales(state)
    SectionHeader(
        stringResource(Res.string.home_scales_title),
        trailing =
            when {
                state.bluetoothDenied -> null
                state.bluetoothOff -> stringResource(Res.string.home_bluetooth_off)
                else -> foundNote(state.scales.size).text()
            },
        trailingColor =
            when {
                state.bluetoothOff -> CgColors.Status.Warning
                state.scales.isEmpty() -> CgColors.Text.Secondary
                else -> CgColors.Text.Primary
            },
        searching = !state.bluetoothOff && !state.bluetoothDenied,
    )
    val settled = rememberListSettled()
    if (scale != null || others.isNotEmpty()) {
        // The connected scale and the ones found as one grid, meeting on single hairlines.
        HairlineStack {
            if (scale != null) {
                val status = if (state.connectingScaleId != null) RowStatus.Connecting else statusOf(state.connection)
                MachineRow(
                    name = scale.name.text(),
                    detail = stringResource(Res.string.home_bluetooth_scale),
                    icon = CgIcons.Bluetooth,
                    status = stringResource(status.label),
                    statusDot = dotOf(status),
                    selected = true,
                    onClick = {},
                    offline = status == RowStatus.Offline,
                    framed = false,
                )
            }
            for (device in others) {
                key(device.id) {
                    Arriving(settled) {
                        ScaleDeviceRow(
                            name = device.name,
                            detail = stringResource(Res.string.home_scale_detail),
                            bars = signalBars(device.advertisement.rssi),
                            action = ScaleAction.Connect,
                            onAction = { onScale(device) },
                            framed = false,
                        )
                    }
                }
            }
        }
    }
    if (state.bluetoothDenied) {
        ConnectScaleCard(
            stringResource(Res.string.home_connect_scale_title),
            stringResource(Res.string.home_connect_scale_subtitle),
            onClick = onAllowBluetooth,
        )
    } else if (state.bluetoothOff) {
        Text(
            stringResource(Res.string.home_turn_on_bluetooth),
            style = CgTheme.type.bodySmall,
            color = CgColors.Text.Secondary,
        )
    } else if (scale == null && others.isEmpty()) {
        SkeletonRow(bars = ScaleSkeletonBars)
    }
}

private fun dotOf(status: RowStatus) =
    when (status) {
        RowStatus.Ready,
        RowStatus.Connected -> CgColors.Status.Live
        RowStatus.Connecting,
        RowStatus.Busy -> CgColors.Status.Warning
        RowStatus.Offline -> CgColors.Text.Secondary
    }

@Composable
private fun AddressSheet(address: String, onAddress: (String) -> Unit, onConnect: () -> Unit, onDismiss: () -> Unit) {
    CgSheet(title = stringResource(Res.string.home_add_by_address), onDismiss = onDismiss) {
        AddressForm(address, onAddress, onConnect)
    }
}

/** The address sheet as a book foldable's right page: the same header and form under a signal rule. */
@Composable
private fun AddressPage(address: String, onAddress: (String) -> Unit, onConnect: () -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxSize().topRule().padding(top = CgStroke.Rule)) {
        SheetHeader(stringResource(Res.string.home_add_by_address), onDone = onDismiss)
        Box(Modifier.padding(CgSpace.S16)) { AddressForm(address, onAddress, onConnect) }
    }
}

/** What adding a machine by its address asks for: the note, the address and connect. */
@Composable
private fun AddressForm(address: String, onAddress: (String) -> Unit, onConnect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S16)) {
        Text(
            stringResource(Res.string.home_address_note),
            style = CgTheme.type.bodySmall,
            color = CgColors.Text.Secondary,
        )
        val label = stringResource(Res.string.home_address_label)
        Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S6)) {
            SectionLabel(label)
            CgTextField(
                value = address,
                onValueChange = onAddress,
                label = label,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onConnect() }),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.HomeAddressField),
            )
        }
        CgButton(
            stringResource(Res.string.home_connect),
            onClick = onConnect,
            kind = CgButtonKind.Signal,
            enabled = address.isNotBlank(),
            modifier = Modifier.fillMaxWidth().testTag(TestTags.HomeAddressConnect),
        )
    }
}

/**
 * A wide Home's second page: the take the record button starts. The source and its live readings, how the take starts
 * and stops (the record screen's own settings), and the record button at the bottom.
 */
@Composable
private fun NextTakePane(
    state: HomeUiState,
    take: () -> NextTake,
    onRecording: ((RecordingSettings) -> RecordingSettings) -> Unit,
    onOpenCamera: () -> Unit,
    onSample: () -> Unit,
) {
    // Only a change of the rules recomposes the pane; the live reading is read by LiveTake alone.
    val settings by remember(take) { derivedStateOf { take().settings } }
    // A short panel (a tablet on its side) tightens up, so the rules still show above the record button.
    BoxWithConstraints(
        Modifier.fillMaxSize().drawBehind {
            drawLine(CgColors.Line.Hairline, Offset.Zero, Offset(0f, size.height), CgStroke.Hairline.toPx())
        }
    ) {
        val compact = maxHeight < COMPACT_PANEL_HEIGHT
        val gap = if (compact) CgSpace.S12 else 20.dp
        Column(
            Modifier.fillMaxSize().padding(if (compact) CgSpace.S16 else CgSpace.S24),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S4)) {
                    SectionLabel(stringResource(Res.string.home_next_take))
                    Text(
                        buildAnnotatedString {
                            append(state.saved?.name?.text() ?: stringResource(Res.string.home_take_camera_only))
                            state.saved?.let {
                                withStyle(SpanStyle(color = CgColors.Text.Secondary)) {
                                    append(" · ")
                                    append(
                                        stringResource(
                                            if (it.isScale) Res.string.source_scale else Res.string.home_take_machine
                                        )
                                    )
                                }
                            }
                        },
                        style = if (compact) CgTheme.type.cardTitle else CgTheme.type.pivot,
                    )
                }
                LiveTake(state, take, compact)
                TakeRules(settings, state.saved?.kind, onRecording)
            }
            val action = recordAction(state)
            val withoutData = state.saved == null || state.connection is ConnectionState.Unreachable
            RecordAction(
                text = if (withoutData) action.text.text() else stringResource(Res.string.home_record),
                caption =
                    if (withoutData) action.caption.text()
                    else if (settings.autoStart) stringResource(Res.string.home_record_armed_auto)
                    else stringResource(Res.string.home_record_armed_manual),
                enabled = action.enabled,
                onClick = onOpenCamera,
                modifier = Modifier.testTag(TestTags.HomeRecordAction),
                withData = !withoutData,
            )
            SampleLink(onSample, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/** The next take's source as it is now: its status line and reading tiles, the only part that follows live samples. */
@Composable
private fun LiveTake(state: HomeUiState, take: () -> NextTake, compact: Boolean) {
    val live = take().live
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CgSpace.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val on = state.connection == ConnectionState.Connected && live != null
            // Read while drawing: the dot's change of colour redraws it, it does not recompose the pane.
            val dot =
                animateColorAsState(
                    if (on) CgColors.Status.Live else CgColors.Text.Dim,
                    CgMotion.medium(),
                )
            Box(Modifier.size(CgSpace.S6).drawBehind { drawRect(dot.value) })
            Text(
                takeStatus(state, live).text(),
                style = CgTheme.type.caption,
                color = CgColors.Text.Secondary,
            )
        }
        val readings = takeReadings(state, live)
        // The tiles open under the status once the machine sends its first reading.
        var shown by remember { mutableStateOf(readings) }
        if (readings.isNotEmpty()) shown = readings
        AnimatedVisibility(
            readings.isNotEmpty(),
            enter = CgMotion.reveal,
            exit = CgMotion.conceal,
        ) {
            // One grid: the tiles meet on single hairlines.
            HairlineGrid(shown.size, columns = shown.size) { i, cell ->
                val r = shown[i]
                ReadingTile(
                    stringResource(r.label),
                    r.value,
                    stringResource(r.unit),
                    cell,
                    color = r.kind.color(),
                    large = !compact,
                    framed = false,
                )
            }
        }
    }
}

/**
 * How the next take starts and stops, all of it, as the record screen's settings have it: the switches, the pre-roll,
 * and while the take stops by itself, on what and after how long. Worded for the [kind] of source: a scale has no pump.
 */
@Composable
private fun TakeRules(
    settings: RecordingSettings,
    kind: SourceKind?,
    onRecording: ((RecordingSettings) -> RecordingSettings) -> Unit,
) {
    val scale = kind == SourceKind.SCALE
    // What this source really runs with: a scale shows "weight settles" where a machine's "shot end" is saved.
    val effective = settings.effective(kind)
    // One framed list, the rules divided by the panel's rule colour, as the canvas lists them.
    Column(Modifier.fillMaxWidth().border(CgStroke.Hairline, CgColors.Line.Hairline)) {
        RuleRow(
            stringResource(Res.string.home_rule_start),
            startRuleDetail(settings, kind).text(),
            checked = settings.autoStart,
            onToggle = { on -> onRecording { it.copy(autoStart = on) } },
            modifier = RuleCell,
        )
        RuleDivider()
        SettingRow(
            stringResource(Res.string.home_rule_pre_roll),
            RuleCell,
            hint =
                stringResource(if (scale) Res.string.record_pre_roll_hint_scale else Res.string.record_pre_roll_hint),
        ) {
            CgStepper(
                value = stringResource(Res.string.record_seconds, settings.preRollSeconds.toString()),
                onMinus = { onRecording { it.copy(preRollSeconds = (it.preRollSeconds - 1).coerceIn(PRE_ROLL)) } },
                onPlus = { onRecording { it.copy(preRollSeconds = (it.preRollSeconds + 1).coerceIn(PRE_ROLL)) } },
                minusEnabled = settings.preRollSeconds > PRE_ROLL.first,
                plusEnabled = settings.preRollSeconds < PRE_ROLL.last,
            )
        }
        RuleDivider()
        RuleRow(
            stringResource(Res.string.home_rule_stop),
            stopRuleDetail(settings, kind).text(),
            checked = settings.stopRule != StopRule.Manual,
            onToggle = { on -> onRecording { it.withStopAfterShot(on) } },
            modifier = RuleCell,
        )
        if (effective.stopRule != StopRule.Manual) {
            RuleDivider()
            // "By hand" is the switch above; the picker holds the rules that stop by themselves.
            val choices =
                StopRuleChoice.entries.filter {
                    it != StopRuleChoice.MANUAL && (kind == null || it.rule().availableFor(kind))
                }
            val current = StopRuleChoice.of(effective.stopRule)
            Column(RuleCell, verticalArrangement = Arrangement.spacedBy(CgSpace.S8)) {
                SegmentedControl(
                    fill = true,
                    options = choices.map { stringResource(it.label) },
                    selectedIndex = choices.indexOf(current),
                    onSelect = { i -> onRecording { it.copy(stopRule = choices[i].rule()) } },
                )
                val label = effective.stopRule.stepLabel
                val parameter = current.parameter
                if (label != null && parameter != null) {
                    SettingRow(stringResource(parameter).replaceFirstChar { it.uppercase() }) {
                        // Stepped from what is shown, so a scale's stand-in "weight settles" becomes the saved rule.
                        CgStepper(
                            value = label,
                            onMinus = {
                                onRecording { s -> s.copy(stopRule = s.effective(kind).stopRule.stepped(-1)) }
                            },
                            onPlus = { onRecording { s -> s.copy(stopRule = s.effective(kind).stopRule.stepped(1)) } },
                        )
                    }
                }
            }
        }
    }
}

/** A cell of the take's framed rules: the canvas's 12 × 16 padding. */
private val RuleCell = Modifier.padding(horizontal = CgSpace.S16, vertical = CgSpace.S12)

/** The rule between two of the take's rules. */
@Composable
private fun RuleDivider() {
    Box(Modifier.fillMaxWidth().height(CgStroke.Hairline).background(CgColors.Line.Divider))
}

private fun RecordingSettings.effective(kind: SourceKind?) = kind?.let { effectiveFor(it) } ?: this

private val PRE_ROLL = RecordingSettings.PRE_ROLL_RANGE

/** Under this the next take's panel is short, and tighter. */
private val COMPACT_PANEL_HEIGHT = 640.dp

@Composable
private fun SectionLabel(text: String) {
    Text(text.caps(), style = CgTheme.type.labelStrong, color = CgColors.Text.Secondary)
}

/** The colour the graph draws this quantity in, so a tile reads as its line. */
private fun ReadingKind.color(): Color =
    when (this) {
        ReadingKind.Temperature -> CgColors.Status.Warning
        ReadingKind.Pressure -> CgColors.Brand.Signal
        ReadingKind.Weight -> BluetoothBlue
        ReadingKind.Flow -> CgColors.Series.Flow
        ReadingKind.WeightFlow -> CgColors.Series.WeightFlow
    }

/**
 * Under the record button: a recorded shot to try the app with when there is no machine at hand (and for App Review,
 * which has none).
 */
@Composable
private fun SampleLink(onSample: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        FooterLink(
            stringResource(Res.string.home_try_sample),
            onClick = onSample,
            modifier = Modifier.testTag(TestTags.HomeSample),
        )
    }
}

/**
 * Behind Start's ? button: the machines and scales OnlyShots knows, how they are found, and the privacy policy and
 * where to get help (guidelines 5.1.1 and 1.5). Stateless, so the screenshot tests can draw it outside its sheet.
 */
@Composable
fun HelpContent(modifier: Modifier = Modifier, onPrivacy: (() -> Unit)? = null, version: String = AppVersion.NAME) {
    val uri = LocalUriHandler.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CgSpace.S24)) {
        Text(
            stringResource(Res.string.home_about_text),
            style = CgTheme.type.body.copy(lineHeight = CgTheme.type.body.fontSize * 1.55f),
        )
        HelpSection(stringResource(Res.string.home_help_machines), stringResource(Res.string.home_help_machines_note)) {
            for (machine in NetworkMachines.all) {
                Row(
                    Modifier.fillMaxWidth().border(CgStroke.Hairline, CgColors.Line.Hairline).padding(CgSpace.S12),
                    horizontalArrangement = Arrangement.spacedBy(CgSpace.S12),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(width = CgSpace.S4, height = CgSpace.S24).background(machineColor(machine.kind)))
                    Text(machine.name.caps(), style = CgTheme.type.label)
                }
            }
        }
        HelpSection(stringResource(Res.string.home_scales_title), stringResource(Res.string.home_help_scales_note)) {
            Text(
                SCALE_BRANDS.caps(),
                style = CgTheme.type.data.copy(lineHeight = CgTheme.type.data.fontSize * 1.6f),
                modifier =
                    Modifier.fillMaxWidth().border(CgStroke.Hairline, CgColors.Line.Hairline).padding(CgSpace.S12),
            )
        }
        Column {
            Box(Modifier.fillMaxWidth().height(CgStroke.Hairline).background(CgColors.Line.Hairline))
            Row(Modifier.fillMaxWidth().padding(top = CgSpace.S8), verticalAlignment = Alignment.CenterVertically) {
                // The app's own privacy screen where the host has one; the published page otherwise. Either ends with
                // where to get help, so one link serves both.
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    FooterLink(
                        stringResource(Res.string.home_privacy),
                        modifier = Modifier.testTag(TestTags.HelpPrivacy),
                    ) {
                        onPrivacy?.invoke() ?: uri.openUri(PRIVACY_URL)
                    }
                    CgIcon(
                        CgIcons.ArrowRight,
                        tint = CgColors.Text.Primary,
                        size = 14.dp,
                        modifier = Modifier.padding(start = CgSpace.S6),
                    )
                }
                Text(
                    stringResource(Res.string.core_version, version),
                    style = CgTheme.type.data,
                    color = CgColors.Text.Secondary,
                )
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, note: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CgSpace.S8)) {
        SectionLabel(title)
        content()
        Text(note, style = CgTheme.type.bodySmall, color = CgColors.Text.Secondary)
    }
}

/** A link in Mono, underlined, 44 dp tall to touch. */
@Composable
private fun FooterLink(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        label,
        style = CgTheme.type.labelStrong.copy(letterSpacing = 0.em, textDecoration = TextDecoration.Underline),
        color = CgColors.Text.Primary,
        modifier =
            modifier.heightIn(min = CgSize.Touch).wrapContentHeight().clickable(role = Role.Button, onClick = onClick),
    )
}

/** A wide Home's first page this wide or more puts machines and scales side by side. */
private val TWO_COLUMNS = 600.dp

/**
 * Where the bars are native (iPhone Duo): record, the screen's prominent action, and adding a machine by its address at
 * the far end. The record key on the screen stays: it is the page's own call to action.
 */
@Composable
private fun HomeNativeBar(canRecord: Boolean, onRecord: () -> Unit, onAddByAddress: () -> Unit) {
    val record = stringResource(Res.string.core_record)
    val address = stringResource(Res.string.home_add_by_address)
    NativeItems(
        listOf(
            NativeBarItem(
                TestTags.HomeRecordAction,
                record,
                BarIcon.Record,
                prominent = true,
                enabled = canRecord,
                priority = BarPriority.High,
            ),
            NativeBarItem(TestTags.HomeAddByAddress, address, BarIcon.Add, bottom = true, priority = BarPriority.Low),
        )
    ) { id ->
        if (id == TestTags.HomeRecordAction) onRecord() else onAddByAddress()
    }
}
