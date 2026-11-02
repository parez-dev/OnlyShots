package app.onlyshots.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import app.onlyshots.camera.CameraState
import app.onlyshots.core.designsystem.CgColors
import app.onlyshots.core.designsystem.CgSpace
import app.onlyshots.core.designsystem.CgTheme
import app.onlyshots.core.designsystem.component.CoffeeScene
import app.onlyshots.core.model.NormRect
import app.onlyshots.core.model.Origin
import app.onlyshots.core.model.OverlaySettings
import app.onlyshots.core.model.RecordingSettings
import app.onlyshots.core.model.Reframe
import app.onlyshots.core.model.SourceKind
import app.onlyshots.core.model.VideoFormat
import app.onlyshots.core.model.VideoItem
import app.onlyshots.core.model.sample.SampleShots
import app.onlyshots.core.model.summary
import app.onlyshots.core.resources.raw
import app.onlyshots.edit.EditActions
import app.onlyshots.edit.EditBody
import app.onlyshots.edit.EditSheet
import app.onlyshots.edit.EditUiState
import app.onlyshots.edit.PlayerState
import app.onlyshots.home.HomeBody
import app.onlyshots.home.HomeUiState
import app.onlyshots.home.MachineShotsBody
import app.onlyshots.home.MachineShotsUiState
import app.onlyshots.home.StartBody
import app.onlyshots.layout.ProvideWindowLayout
import app.onlyshots.open.ChosenShot
import app.onlyshots.open.OpenBody
import app.onlyshots.open.OpenEntry
import app.onlyshots.open.OpenHost
import app.onlyshots.open.OpenUi
import app.onlyshots.open.PickedFile
import app.onlyshots.open.ShotOrigin
import app.onlyshots.open.StoreLinks
import app.onlyshots.overlay.GraphStyles
import app.onlyshots.platform.SavedVideo
import app.onlyshots.privacy.PrivacyScreen
import app.onlyshots.record.RecordActions
import app.onlyshots.record.RecordBody
import app.onlyshots.record.RecordPhase
import app.onlyshots.record.RecordUiState
import app.onlyshots.record.RecordingSettingsContent
import app.onlyshots.record.formatThatFits
import app.onlyshots.sources.ConnectionState
import app.onlyshots.sources.machine.FoundMachine
import app.onlyshots.sources.machine.Gaggiuino
import app.onlyshots.sources.machine.NetworkMachines
import app.onlyshots.style.StyleEditorBody
import app.onlyshots.style.StyleTab
import kotlin.time.Instant
import uk.co.fractalmotion.mugshot.annotations.Mugshot
import uk.co.fractalmotion.mugshot.annotations.MugshotDevice
import uk.co.fractalmotion.mugshot.annotations.MugshotDevices

/**
 * Whole screens, through their stateless bodies, on a phone and a 1280 × 800 tablet: the same `windowLayout` decision a
 * real window gets picks the phone layout or the wide panes. The camera and the video are the drawn espresso scene;
 * blur and glass styles render without their blur here (layoutlib has no Haze), as the overlay previews do.
 */
@Mugshot @MugshotDevices(MugshotDevice.PHONE, MugshotDevice.TABLET) annotation class ScreenShots

@Composable internal fun Screen(content: @Composable () -> Unit) = CgTheme { ProvideWindowLayout { content() } }

/** A video of the Zer0 shot, as the library holds one: 47 s, the shot starting 3 s in. */
internal val sampleVideo =
    VideoItem(
        id = "demo",
        file = "espresso-demo.mp4",
        createdAt = Instant.parse("2026-10-18T08:14:00Z"),
        durationUs = 47_000_000,
        origin = Origin.IMPORTED,
        hasAudio = true,
        shot = SampleShots.zer0,
        shotStartInVideoUs = 3_000_000,
        syncLocked = false,
        trimInUs = 0,
        trimOutUs = 47_000_000,
        overlay = OverlaySettings(),
    )

internal val picture: @Composable (Modifier) -> Unit = { CoffeeScene(it, progress = 0.6f) }

// Start -----------------------------------------------------------------------------------

@ScreenShots @Preview @Composable internal fun Start() = Screen { StartBody(onRecord = {}, onEdit = {}) }

// Home ------------------------------------------------------------------------------------

@ScreenShots @Preview @Composable internal fun HomeSearching() = Screen { HomeBody(HomeUiState(), {}, {}, {}, {}, {}) }

@ScreenShots
@Preview
@Composable
internal fun HomeMachineFound() = Screen {
    val machine = FoundMachine(NetworkMachines.all.first(), "10.0.1.42", 80, "gaggiuino.local")
    HomeBody(HomeUiState(machines = listOf(machine)), {}, {}, {}, {}, {})
}

// Record ----------------------------------------------------------------------------------

@Composable
internal fun Record(
    state: RecordUiState,
    settings: RecordingSettings = RecordingSettings(),
    granted: Boolean = true,
    graphSelected: Boolean = false,
) {
    // The camera films the way the screen stands, as the record screen asks it to.
    val window = LocalWindowInfo.current.containerSize
    val capture = formatThatFits(window.width, window.height)
    RecordBody(
        state = state,
        camera = CameraState(previewReady = true, minZoom = 0.6f, maxZoom = 10f, lenses = listOf(0.6f, 1f, 3f)),
        // The saved graph is kept in a 9:16 frame; the screen's view model moves it into the one being filmed.
        settings =
            settings.copy(overlay = settings.overlay.copy(rect = settings.overlay.rect.refit(PORTRAIT, capture))),
        graphStyle = GraphStyles.Gaggiuino,
        customStyles = emptyList(),
        granted = granted,
        canSimulateShot = false,
        saveError = null,
        actions = RecordActions.None,
        onBack = {},
        onStyleEditor = {},
        picture = { modifier, _ -> picture(modifier) },
        capture = capture,
        live = { state.shot.samples.lastOrNull() },
        graphSelectedAtFirst = graphSelected,
    )
}

private val PORTRAIT = VideoFormat.PORTRAIT_9_16

/** Armed before the shot, the machine sending: what the record screen shows most of the time. */
internal val armedTake =
    RecordUiState(
        phase = RecordPhase.ARMED,
        status = "armed · waiting for the shot".raw(),
        connection = ConnectionState.Connected,
        sourceKind = SourceKind.GAGGIUINO,
        shot = SampleShots.zer0.copy(samples = SampleShots.zer0.samples.filter { it.tUs <= 15_000_000 }),
        stopRuleText = "stops 5 s after the shot".raw(),
    )

@ScreenShots
@Preview
@Composable
internal fun RecordShotRunning() = Screen {
    Record(
        RecordUiState(
            phase = RecordPhase.RECORDING,
            status = "recording · 15.0 s".raw(),
            connection = ConnectionState.Connected,
            sourceKind = SourceKind.GAGGIUINO,
            // A live shot has only the samples received so far.
            shot = SampleShots.zer0.let { z -> z.copy(samples = z.samples.filter { it.tUs <= 15_000_000 }) },
            videoTimeUs = 18_000_000,
            shotStartInVideoUs = 3_000_000,
            stopRuleText = "stops 5 s after the shot".raw(),
        )
    )
}

@ScreenShots
@Preview
@Composable
internal fun RecordWithoutMachine() = Screen {
    Record(
        RecordUiState(
            phase = RecordPhase.NOT_REACHABLE,
            status = "no machine".raw(),
            connection = ConnectionState.Unreachable("no machine set up"),
            sourceKind = SourceKind.GAGGIUINO,
            shot = SampleShots.zer0.copy(samples = emptyList()),
            stopRuleText = "stops 5 s after the shot".raw(),
        )
    )
}

/** The camera refused: the picture asks for it, and the record button is off. */
@ScreenShots
@Preview
@Composable
internal fun RecordNoCamera() = Screen {
    Record(
        RecordUiState(
            phase = RecordPhase.ARMED,
            status = "no camera · allow it to record".raw(),
            connection = ConnectionState.Connected,
            sourceKind = SourceKind.GAGGIUINO,
            shot = SampleShots.zer0.copy(samples = emptyList()),
            stopRuleText = "stops 5 s after the shot".raw(),
            noCamera = true,
        ),
        granted = false,
    )
}

/** The graph tapped before a shot: its toolbar, for the style, the data, its background and its place. */
@ScreenShots
@Preview
@Composable
internal fun RecordGraphSelected() = Screen { Record(armedTake, graphSelected = true) }

/** On its side the camera films 16:9: the button moves to the edge the thumb is at. */
@Mugshot
@MugshotDevices(MugshotDevice.LANDSCAPE)
@Preview
@Composable
internal fun RecordLandscape() = Screen { Record(armedTake) }

@ScreenShots
@Preview
@Composable
internal fun RecordingSettings() = Screen {
    Box(Modifier.fillMaxSize().background(CgColors.Surface.Bar).padding(CgSpace.S16)) {
        RecordingSettingsContent(RecordingSettings(), SourceKind.GAGGIUINO, onChange = {})
    }
}

// Edit ------------------------------------------------------------------------------------

@Composable
internal fun Edit(
    sheet: EditSheet?,
    video: VideoItem = sampleVideo,
    reframing: Boolean = false,
    exporting: Float? = null,
    saved: Boolean = false,
    selected: Boolean = !reframing,
    askNotifications: Boolean = false,
    savedVideo: SavedVideo? = null,
) = Screen {
    EditUi(
        sheet = sheet,
        video = video,
        reframing = reframing,
        exporting = exporting,
        saved = saved,
        selected = selected,
        askNotifications = askNotifications,
        savedVideo = savedVideo,
    )
}

/** [Edit] without its own window layout, for a preview that hands it one. */
@Composable
internal fun EditUi(
    sheet: EditSheet?,
    video: VideoItem = sampleVideo,
    reframing: Boolean = false,
    exporting: Float? = null,
    saved: Boolean = false,
    selected: Boolean = !reframing,
    askNotifications: Boolean = false,
    savedVideo: SavedVideo? = null,
) {
    val player = remember { PlayerState().apply { seek(18_000_000) } }
    EditBody(
        ui =
            EditUiState(
                item = video,
                style = GraphStyles.Gaggiuino,
                sheet = sheet,
                canUndo = true,
                canRedo = true,
                exporting = exporting,
                exportLeftMs = exporting?.let { 31_000L },
                savedToGallery = saved,
                askExportNotifications = askNotifications,
                savedVideo = savedVideo,
            ),
        customStyles = emptyList(),
        player = player,
        actions = EditActions.None,
        onBack = {},
        onStyleEditor = {},
        onExported = {},
        onPickShotFile = {},
        onSaveToGallery = {},
        picture = picture,
        initiallySelected = selected,
        initiallyReframing = reframing,
    )
}

@ScreenShots @Preview @Composable internal fun EditGraphSelected() = Edit(sheet = null)

/** Nothing selected: on a wide window the panel holds the video's shot, frame and export. */
@ScreenShots @Preview @Composable internal fun EditNothingSelected() = Edit(sheet = null, selected = false)

@ScreenShots @Preview @Composable internal fun EditStyleSheet() = Edit(sheet = EditSheet.STYLE)

/** The data sheet: what the graph shows, then what it says. */
@ScreenShots
@Preview
@Composable
internal fun EditDataSheet() =
    Edit(
        sheet = EditSheet.DATA,
        video =
            sampleVideo.copy(overlay = OverlaySettings(title = "Sunday shot", recipe = "18 g in · 36 g out · 28 s")),
    )

/** Exporting: on a wide window the progress sits where the export button was; on a phone, in the export sheet. */
@ScreenShots @Preview @Composable internal fun EditExporting() = Edit(sheet = EditSheet.EXPORT, exporting = 0.42f)

/** Saved: a banner on a wide window; the export sheet says so on a phone. */
@ScreenShots @Preview @Composable internal fun EditSaved() = Edit(sheet = EditSheet.EXPORT, saved = true)

/** The first export: it offers to say outside the app when the video is done. */
@ScreenShots
@Preview
@Composable
internal fun EditExportingAsk() = Edit(sheet = EditSheet.EXPORT, exporting = 0.18f, askNotifications = true)

/** Saved, with the gallery's way back to it: open it there (and share it from there). */
@ScreenShots
@Preview
@Composable
internal fun EditSavedOpen() =
    Edit(sheet = EditSheet.EXPORT, saved = true, savedVideo = SavedVideo("content://media/external/video/media/1"))

/** The editor at 1:1 and at 16:9: the picture in the video's shape, the graph refitted into it. */
@ScreenShots
@Preview
@Composable
internal fun EditSquare() =
    Edit(
        sheet = null,
        video =
            sampleVideo.copy(
                format = VideoFormat.SQUARE_1_1,
                overlay =
                    OverlaySettings(rect = NormRect.Default.refit(VideoFormat.PORTRAIT_9_16, VideoFormat.SQUARE_1_1)),
            ),
    )

@ScreenShots
@Preview
@Composable
internal fun EditLandscape() =
    Edit(
        sheet = null,
        video =
            sampleVideo.copy(
                format = VideoFormat.LANDSCAPE_16_9,
                overlay = OverlaySettings(rect = NormRect.default(VideoFormat.LANDSCAPE_16_9)),
            ),
    )

@ScreenShots
@Preview
@Composable
internal fun EditReframing() =
    Edit(sheet = null, video = sampleVideo.copy(reframe = Reframe(zoom = 1.8f, x = 0.45f, y = 0.6f)), reframing = true)

// Style editor ----------------------------------------------------------------------------

@ScreenShots
@Preview
@Composable
internal fun StyleEditorPanel() = Screen {
    StyleEditorBody(draft = GraphStyles.Aero, frames = emptyList(), onChange = {}, onBack = {}, onSave = {})
}

@ScreenShots
@Preview
@Composable
internal fun StyleEditorGlass() = Screen {
    StyleEditorBody(
        draft = GraphStyles.LiquidGlass,
        frames = emptyList(),
        onChange = {},
        onBack = {},
        onSave = {},
        initialTab = StyleTab.PANEL,
    )
}

/** The text tab: title, labels, the effect under the text, the phase colours. */
@ScreenShots
@Preview
@Composable
internal fun StyleEditorText() = Screen {
    StyleEditorBody(
        draft = GraphStyles.Minimal,
        frames = emptyList(),
        onChange = {},
        onBack = {},
        onSave = {},
        initialTab = StyleTab.TEXT,
    )
}

/** Metro's tiles, made in the editor: the readings tab with a tile colour per reading. */
@ScreenShots
@Preview
@Composable
internal fun StyleEditorTiles() = Screen {
    StyleEditorBody(
        draft = GraphStyles.Metro,
        frames = emptyList(),
        onChange = {},
        onBack = {},
        onSave = {},
        initialTab = StyleTab.READINGS,
    )
}

// Open: the start of the editor on desktop and web, "compose a video" on the phones --------------

private val webHost =
    OpenHost(
        headerNote = "runs in your browser, nothing is uploaded",
        videosNote =
            "Exports at 1080p in Chrome, Edge or Safari 16.4+. A .mov from an iPhone holds HEVC, which not " +
                "every browser can decode.",
    )

private val desktopHost =
    OpenHost(
        headerNote = "runs on this computer, nothing is uploaded",
        videosNote = "Exports at the video's full quality, with sound, into Movies/OnlyShots.",
    )

internal val pickedVideo =
    sampleVideo.copy(shot = null, recordedAt = Instant.parse("2026-10-20T08:14:00Z"), durationUs = 42_000_000)

internal val pickedFile = PickedFile("morning-shot-zer0.mov", 1080, 1920)

/** A computer's browser: both stores. */
private val computerStores = StoreLinks(onPlay = {}, onAppStore = {}, phone = false)

/** An Android phone's browser: Google Play, the banner open. */
private val androidStores = StoreLinks(onPlay = {}, onAppStore = null, phone = true)

/** The web's start in a computer's browser, nothing picked yet: the app's band under the steps. */
@ScreenShots
@Preview
@Composable
internal fun OpenEmpty() = Screen { OpenBody(OpenUi(fromMachine = false), OpenEntry.Desk(webHost, computerStores)) }

/** The web on a phone (phone size) or a tablet (tablet size): the banner, or the band with one store. */
@ScreenShots
@Preview
@Composable
internal fun OpenMobileWeb() = Screen { OpenBody(OpenUi(fromMachine = false), OpenEntry.Desk(webHost, androidStores)) }

/** The phone's banner closed: the header's "get the app" link instead. */
@ScreenShots
@Preview
@Composable
internal fun OpenMobileWebClosed() = Screen {
    OpenBody(OpenUi(fromMachine = false), OpenEntry.Desk(webHost, androidStores.copy(bannerDismissed = true)))
}

/** The phone's page with the video picked: the shot is next. */
@ScreenShots
@Preview
@Composable
internal fun OpenMobileWebPicked() = Screen {
    OpenBody(
        OpenUi(picked = pickedVideo, pickedFile = pickedFile, fromMachine = false),
        OpenEntry.Desk(webHost, androidStores),
    )
}

/** The design's state: the video picked, the shot next. */
@ScreenShots
@Preview
@Composable
internal fun OpenPicked() = Screen {
    OpenBody(OpenUi(picked = pickedVideo, pickedFile = pickedFile, fromMachine = false), OpenEntry.Desk(webHost))
}

/** Both picked: "open editor" lights up. */
@ScreenShots
@Preview
@Composable
internal fun OpenReady() = Screen {
    OpenBody(
        OpenUi(
            picked = pickedVideo,
            pickedFile = pickedFile,
            shot = ChosenShot(SampleShots.zer0, "shot-1287.json", ShotOrigin.File),
            fromMachine = false,
        ),
        OpenEntry.Desk(webHost),
    )
}

/** The desktop's start: a shot from a file or the machine. */
@ScreenShots
@Preview
@Composable
internal fun OpenEmptyDesktop() = Screen { OpenBody(OpenUi(), OpenEntry.Desk(desktopHost)) }

/** A video held over the window. */
@ScreenShots
@Preview
@Composable
internal fun OpenDragging() = Screen { OpenBody(OpenUi(dragging = true, fromMachine = false), OpenEntry.Desk(webHost)) }

@ScreenShots
@Preview
@Composable
internal fun EditVideoEmpty() = Screen { OpenBody(OpenUi(), OpenEntry.PhoneApp(onBack = {})) }

@ScreenShots
@Preview
@Composable
internal fun EditVideoPicked() = Screen {
    OpenBody(
        OpenUi(
            picked = sampleVideo.copy(shot = null),
            shot = ChosenShot(SampleShots.zer0, SampleShots.zer0.profileName, ShotOrigin.Machine),
        ),
        OpenEntry.PhoneApp(onBack = {}),
    )
}

@ScreenShots
@Preview
@Composable
internal fun MachineShotsSearching() = Screen {
    MachineShotsBody(MachineShotsUiState(), onMachine = {}, onShot = {}, onBack = {})
}

@ScreenShots
@Preview
@Composable
internal fun MachineShotsList() = Screen {
    val machine = FoundMachine(Gaggiuino, "10.0.1.42", 80, hostname = "gaggiuino.local")
    val shots =
        listOf(
            SampleShots.zer0.summary(),
            SampleShots.zer0.copy(id = "120", profileName = "Flimsy Light").summary(),
            SampleShots.zer0.copy(id = "119", profileName = "Londinium").summary(),
        )
    MachineShotsBody(
        MachineShotsUiState(machines = listOf(machine), machine = machine, shots = shots),
        onMachine = {},
        onShot = {},
        onBack = {},
    )
}

// Privacy --------------------------------------------------------------------------------------

@ScreenShots @Preview @Composable internal fun Privacy() = Screen { PrivacyScreen(onBack = {}, version = "1.0.0") }
