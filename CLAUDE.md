# CLAUDE.md

Guide for Claude Code in this repo.

OnlyShots films espresso shot with graph on top (pressure, flow, weight…). Live or synced after. Exports MP4.
Compose Multiplatform: Android, iOS. Same Compose editor on desktop (JVM, FFmpeg) and web (wasm + JS). README.md =
product page + machines; docs/DEVELOPMENT.md = long reference. Figma = behaviour reference; spec cards' acceptance
criteria = test cases.

## Secrets

None in repo. No setup. Keystores and `local.properties` are in `.aiexclude`: no read, no edit. No flavors, no build
variants. Only CI secret: `VERCEL_TOKEN` (org secret, web deploy on `main`).

## Commands

```
./gradlew testAndroidHostTest                          # shared tests, JVM (CI)
./gradlew :core:model:testAndroidHostTest --tests 'app.onlyshots.core.model.SyncTest'           # one class
./gradlew :feature:record:testAndroidHostTest --tests 'app.onlyshots.record.RecordSessionTest'  # one class
./gradlew iosSimulatorArm64Test                        # shared tests + AVFoundation export, simulator
./gradlew :video:connectedAndroidDeviceTest            # Media3 export alignment, device
./gradlew :video:desktopTest                           # FFmpeg export, probe, player, JVM
./gradlew :screenshots:verifyMugshot                   # screenshots, JVM, no device
./gradlew :screenshots:recordMugshot                   # re-record after wanted visual change
tools/ios-screenshots/run.sh                           # iPhone Duo goldens, every pose, simulator (record: re-record)
./gradlew ktfmtCheck                                   # format gate. Fix: ktfmtFormat
./gradlew :uitests:desktopTest                         # UI flows, phone app + editor, every layout (PR gate)
./gradlew :app:desktopApp:test                         # desktop app starts on Open
./gradlew :app:uitests:connectedAndroidTest            # Android smoke, UI Automator, device/emulator
(cd tools/web-smoke && node smoke.mjs)                 # web smoke, Playwright, after the web build
./gradlew :app:androidApp:assembleDebug                # Android debug APK
./gradlew :app:androidApp:assembleRelease              # R8 on; signed only with ONLYSHOTS_KEY* env
./gradlew :app:androidApp:generateBaselineProfile     # baseline profile, phone API 33+ (ANDROID_SERIAL=…)
./gradlew :app:baselineprofile:connectedBenchmarkReleaseAndroidTest  # cold start with vs without it
./gradlew :app:desktopApp:run                          # desktop editor
./gradlew :app:desktopApp:hotRun --auto                # same, Compose Hot Reload
./gradlew :app:webApp:composeCompatibilityBrowserDistribution  # web: wasm + JS fallback
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun      # web wasm, http://localhost:8080
./gradlew :app:webApp:jsBrowserDevelopmentRun          # web JS, same port
./gradlew bumpVersion -Ppart=minor                     # version (also -Pto=X.Y.Z)
./gradlew checkAppVersion                              # iOS files match version?
tools/deploy-web.sh                                    # build, publish onlyshots.vercel.app
```

iOS: `cd app/iosApp && xcodegen generate`, build scheme `iosApp`. Swift sees `:app:shared` as Swift export module
`OnlyShotsApp` (built by the pre-build phase, `embedSwiftExportForXcode`). Min iOS 18: Swift export's floor.

Format: ktfmt `kotlinLangStyle`, 4 spaces, width 120. Run `ktfmtFormat` before commit.

## Build quirks

- **Layout:** apps in `app/` (`:app:shared`, `:app:androidApp`, `:app:desktopApp`, `:app:webApp`, `app/iosApp`,
  `:app:baselineprofile`).
  Libraries at root. Entry modules hold only entry code + packaging. Shared code in `:app:shared` or below.
- **AGP 9:** libraries use `com.android.kotlin.multiplatform.library`. Test sets: `androidHostTest` (JVM),
  `androidDeviceTest`. Not `test`/`androidTest`. `:app:androidApp` = plain `com.android.application`, built-in Kotlin,
  no Kotlin plugin.
- **Resources gap:** plugin no package library Compose resources into APK or host tests.
  `app/androidApp/build.gradle.kts` copies `:core:resources` into assets. `screenshots/build.gradle.kts` puts them
  on classpath. Path: `composeResources/app.onlyshots.core.resources/`.
- **Repos:** `settings.gradle.kts` uses `PREFER_SETTINGS`. Wasm toolchain adds Node/Yarn/Binaryen repos itself.
- **Convention plugins** (`build-logic/`): `onlyshots.kmp.library` (Android, iOS, `jvm("desktop")`, namespace from
  path), `onlyshots.kmp.wasm` (wasmJs + js), `onlyshots.compose`, `onlyshots.feature` (library + Compose + `:core:ui`,
  lifecycle, Koin), `onlyshots.versioning`. Shared setting? Change there, not per module.
- **Desktop:** runs `EditorApp()`. FFmpeg from JavaCV, host natives only (`desktopNatives` in
  `video/build.gradle.kts`): build runs on OS built on. No camera, machines, scales. Packaged app has trimmed JDK
  (`modules(...)` in `app/desktopApp/build.gradle.kts`): new JDK API → `suggestRuntimeModules` + launch packaged app.
- **Baseline profile:** `:app:baselineprofile` (`com.android.test`) runs UI Automator journeys (`Journeys.kt`: start,
  then record the sample shot and export it) and writes `app/androidApp/src/release/generated/baselineProfiles/`, checked
  in; release builds never regenerate it. Journeys find buttons by their words: reword one, regenerate. ProfileInstaller
  installs it outside the store.
- **Swift export** (`export { swift { } }` in `app/shared/build.gradle.kts`, Beta): Swift gets native names
  (`MainViewController()`, `platform.IosBackground.shared`). Swift-facing API = `:app:shared` iosMain only; its `api`
  dependencies are `HIDDEN` there (exporting them breaks on okio). A Swift class implementing a Kotlin interface
  subclasses `KotlinBase`. Read the generated `build/SPMPackage/.../OnlyShotsApp.swift` after changing that API.
- **R8** breaks reflection quietly. Touch keep rules or serialization? Walk the six release paths in
  docs/DEVELOPMENT.md by hand.

## CI

- **`pr.yml`** (every PR): `ktfmtCheck`, `checkAppVersion`, `testAndroidHostTest`, `:screenshots:verifyMugshot`,
  `:uitests:desktopTest`, `:app:desktopApp:test`. Branch PRs also `duo-screenshots` (`tools/ios-screenshots/run.sh`,
  iPhone Duo simulator, self-hosted Mac only).
  Branch PRs on self-hosted Mac (`self-hosted`, `macOS`, `ARM64`). Fork PRs on `ubuntu-latest`: fork code never runs on
  the Mac. Keep that guard.
- **`ci.yml`** (push to `main`): full build, all platforms, export tests, UI smoke tests (Android emulator, iOS
  XCUITest, web Playwright), iPhone Duo goldens (self-hosted Mac), web deploy.
- **`release.yml`** (tag `v*`): signed Android bundle to Play internal, desktop installers, GitHub Release.

## Version and release

- **One version:** `onlyshots.version` in `gradle.properties`, semver, parsed by kotlin-semver in build-logic
  (`AppVersion.kt`). Feeds Android name/code, desktop package, iOS `MARKETING_VERSION`/`CURRENT_PROJECT_VERSION`,
  generated `app.onlyshots.core.model.AppVersion` (shown in help sheet, privacy screen).
- **Build number:** `major × 10000 + minor × 100 + patch`. Same version twice to store? Bump first.
- **Change only with `bumpVersion`:** it rewrites iOS files too. `checkAppVersion` (CI) fails on drift. Previews
  pass fixed version, so bumps never move golden. macOS no take 0.x: stay ≥ 1.0.0.
- **Release = tag `v<version>`:** `.github/workflows/release.yml` checks tag, uploads signed AAB to Play internal
  track (upload key from env `ONLYSHOTS_KEY*`, secrets in docs/DEVELOPMENT.md), builds desktop DMG/MSI/DEB
  (unsigned), publishes GitHub Release. Web deploys from `main` (`ci.yml`). iOS upload manual.

## Architecture

**Modules.** `:core:model` → `:overlay` → `:overlay:compose` → `:video` → `:core:ui` → `:feature:style` →
`:feature:edit` → `:feature:record`. `:feature:open`, `:feature:home` beside. `:editor` = whole editor (Open → Edit
→ style editor + `editorModule`), used by desktop and web. `:app:shared` = phones' `OnlyShotsApp` (Start → Home →
Record → Edit), `appModule`, `initKoin`, `PlatformModule`s, iOS entry. `:core:ui` = shared screen stuff: adaptive
layout, routes (Navigation 3 `NavKey`s, `RouteSaving`, helpers `go`/`back`/`backTo`/`replaceAbove`, `NavResults` in
`Navigation.kt`), platform `expect` composables (`VideoSurface`, pickers, permissions, gallery), interfaces
(`PlatformShare`, `VideoGallery`, `BackgroundWork`), `DevTools.enabled`.

**Look: "Instrument".** Ink `#0B0B0B`, bone text, one signal `#FF5A1F` accent, hairlines, square corners. Plex Mono
names and counts (capitals via `String.caps()`, never in XML), Plex Sans sentences. Tokens only (`Tokens.kt`,
`Type.kt`): no raw shapes, colours, font sizes in features. Canvas "OnlyShots · Instrument" = reference.

**UI tests find by `TestTags`.** Every control a test drives has a `Modifier.testTag(TestTags.X)` (in
`core/designsystem/.../TestTags.kt`); screens are wrapped in `TaggedScreen`. Rename a tag only with its tests. A new
dialog/sheet/menu window: `Modifier.testTagsAsResourceIds()` on it, or Android tests can't see inside.

**Resources.** All in `:core:resources` (`Res` in `app.onlyshots.core.resources`): fonts, icons and illustrations as
vector XML (`drawable/ic_*`, `illus_*`), every user string in `values/strings_<area>.xml` (keys prefixed:
`home_`, `edit_`, `record_`, …).

- **New text:** goes in XML. Never literal. Proper nouns (machine, scale, built-in style names), glyphs, number
  formats stay code.
- **Outside composition:** use `UiText` (`Res.string.x.text(args)`, `.raw()`). `text()` in composable, `resolve()`
  elsewhere. Tests compare `UiText`: resources no resolve in host tests.
- **Domain modules** (`:core:model`, `:sources`, `:camera`, `:storage`, `:overlay`) no resources. They carry meaning
  (enum, `CameraError`, `OverlaySnap.Place`); UI words it.
- **XML:** write `'` and `"` plain. Compose resources no unescape `\'`.
- **Icons:** `CgIcons`, from vector drawables.

**One scene builder, one painter — "export matches preview".** `buildOverlayScene(shot, time, settings, style)` in
`:overlay` is pure. Flat rects, polylines, areas, text in frame pixels, from 390-wide reference. `:overlay` has no
Compose, no resources: words arrive as `OverlayLabels`, resolved once by `rememberOverlayLabels()` (screens) or
`overlayLabels()` (each exporter, before frame loop). Fonts likewise: `OverlayFonts(sans, mono)` from
`CgTheme.overlayFonts` / `loadOverlayFonts()`; the painter picks the style's `text.font`. `drawOverlay` in `:overlay:compose` is only code drawing those
primitives: record, editor, screenshots, `OverlayFrameRenderer` (export), every platform. New visual element → scene
builder, not renderer.

**Styles are data** (`core/model/.../style/GraphStyle.kt`). Built-ins in `GraphStyles` (Instrument first), user styles in
`AppSettings.customStyles` via `StyleLibrary`, copy in video's `OverlaySettings.style`. Resolve with
`GraphStyles.resolve(overlay, source, custom, defaults)` / `StyleLibrary.resolve`. Never by id alone. Style editor
changes styles only via `feature/style/.../StyleEdits.kt`. `BuiltInsAreReplicableTest` rebuilds every built-in from
another with those alone: new style field needs edit + control, or test fail.

- **Blur exception:** `BackdropPrim` = blur video here. Preview: Haze draws it; picture under must be Compose
  `hazeSource` (TextureView on Android, frames on iOS, desktop, web). Export: each exporter blurs those rects
  (`BackdropBlurEffect` Android, `SkiaFrameDrawer` elsewhere).
- **Glass:** `GlassSettings` (Liquid demo's eight controls) mapped once by `toHaze` in `GlassMapping.kt`. Preview
  gives Haze (`toHazeStyle`); exporters give Haze's vendored shader (`overlay/.../glass/`, SkSL for Skia, GLSL for
  Android). Change both sides together.

**Formats.** `VideoFormat` (9:16, 4:5, 1:1, 16:9; `VideoItem.format`). Output size
`Quality.size(format)` (1080 or 2160 short side). Overlay unit `u = min(w, h) / 390`. `NormRect`, `Reframe` =
fractions of output frame. Default rect, min size, snap targets per format (`NormRect.default/minW/minH/refit`,
`OverlaySnap.snap(…, format)`). `sourcePoint` maps output point to source (cover crop, then reframe), as every
exporter and `VideoSurface` draws. Recording picks no format: camera films whole frame the way screen stands
(`formatThatFits` → `RecordViewModel.captureFormat`, 9:16 or 16:9; turned while armed = re-arm), saved as video's
format; editor crops. Screen held only while take runs (`LockOrientation`; iOS via app delegate).
`CameraController.setFormat` turns iOS writer. Saved record graph rect kept in 9:16 frame, refit to capture frame.

**Export** per platform in `:video`:

- **Android:** Media3 Transformer + `OverlayEffect`.
- **iOS:** `AVAssetReader` → Skia → `AVAssetWriter` pixel buffers.
- **Desktop:** FFmpeg (JavaCV) → Skia → FFmpeg.
- **Web:** Mediabunny + WebCodecs (`app/webApp/.../media.js`), 1080×1920, silent.

iOS, desktop, web share `SkiaFrameDrawer` (`video/src/skikoMain`). `reframe` = `graphicsLayer` in preview, Media3
`MatrixTransformation`, `SkiaFrameDrawer` transform, `media.js` cover draw: same mapping as `Reframe.map`. Alignment
proved by barcode test clip (`Media3ExportTest`, `AvFoundationExportTest`, `FfmpegExportTest`).

**Background export.** `ExportManager` (Koin single, own scope) renders inside `BackgroundWork.keepAlive(ExportTask)`,
reports progress about once a second, keeps each video's `Outcome` until the editor `consume`s it, and ends with
`BackgroundWork.finished(ExportEnd)`. Android: `ExportService` (foreground, `mediaProcessing`/`dataSync`) whose
notification is the progress (`ExportNotifications`, Live Update on Android 16), Cancel via `ExportCancelReceiver`, a
result notification that plays the MediaStore video. iOS: `ContinuedExport.swift` (iOS 26 `BGContinuedProcessingTask`,
system progress) else `beginBackgroundTask`, banner via `UNUserNotificationCenter`. Web: tab title + leave warning +
desktop `Notification`. A tap opens the editor through `OpenRequests`. Words resolved before, built on the main thread.

**Live data.** Every machine or scale = `ShotSource` in `:sources`. Network machines over WebSocket; scales = pure
`BleProtocol` over Kable. Network machine = `NetworkMachine` (`sources/.../machine/`) in `NetworkMachines.all`
(today only Gaggiuino). Home, saved settings, machine log go through registry. `NetworkScan` finds machines (mDNS on
Android, /24 sweep, fixed `.local` names last), confirms each with `identify`. `SourceKind.GAGGIMATE/DECENT` stay on
purpose: old sidecars and settings name them. Each decoder pinned to upstream commit in doc comment: check before
changing format. `SourceHolder` swaps live source under every screen. Every build starts disconnected. Only
Home's "try a sample shot" switches to the Zer0 replay (`SourceHolder.useSample`, any build, how App Review tries app),
for one take: saving it or recording without data ends it (`endSample`). **Simulate shot** only then.

**New machine?** Follow README "Add a new machine":

1. New `SourceKind` value (never rename/remove); fill the `when`s compiler flags: `Capabilities.of`, `machineName`,
   `machineColor` + `CgColors.Machine.*`, `RecordSession.triggerText` + `record_trigger_*` string.
2. In `sources/.../machine/`: `MachineDecoder`, `NetworkMachine` object (`identify` fast, closes socket), `source`
   → `WebSocketShotSource`, optional `history` → `MachineHistory`. Append to `NetworkMachines.all` (`all.first()` =
   default).
3. Tests mirroring Gaggiuino's (decoder frames, `NetworkScanTest` fake, Ktor fake machine started outside
   `runBlocking`). Pin upstream commit in decoder doc comment. Add README machines-table row.

**Recording.** `RecordSession` (`feature/record/`) = state machine (armed → recording → saved, plus manual,
not-reachable). Drives auto-start and stop rules from `:core:model`. Records from arm: pre-roll in file, `trimInUs`
cuts idle head.

**Storage: sidecars are only truth.** JSON sidecar per video (schema `core/model/Sidecar.kt`, atomic writes) beside
the video. No database: no screen lists the library. Picked video enters via `VideoImport`: copy on phones/desktop,
alias of browser `File` on web.

**Adaptive layout.** One function: `windowLayout(info, widthDp, heightDp, density)` → `Phone`/`Wide`/`Stacked`/`Tiny`
+ split. Input: `currentWindowAdaptiveInfo()` (size classes, Android hinges). iOS: `platformWindowAdaptiveInfo()` adds
foldable iPhone fold from `IosHinge`, fed by Swift `ComposeHostController` (`NativeShell.swift`; Kotlin UIKit bindings
lack iOS 27.1 hinge API). Every adaptive branch goes through it (`LocalWindowLayout`). No ad hoc size checks.

- **`Wide`:** `WidePanes`. On fold (`WindowLayout.fold`): two pages meeting at fold. Else picture beside panel or
  above it, whichever gives picture more room (`placePanel`), where phone's bottom sheets go.
- **Record screen:** no panes. `Phone` and `Wide` = camera fills window, `FloatingControls` over it, settings and
  graph pages in bottom sheets. Graph kept where screen shows it (`OverlayEditLayer(bounds)`).
- **Foldables:** follow "OnlyShots Foldables" canvas. Previews in `screenshots/.../FoldablePreviews.kt` hand posture
  directly. `TwoPane` only for tabletop (`Stacked`). Remaining bottom sheets capped 640 dp.
- **iPhone Duo native shell** (iOS 27.1, Figma "OnlyShots · iPhone Duo"): Swift `ShellNavigationController` around
  the one Compose screen. Where there is a hinge, system shows each screen's bar (side column on outer and inner
  landscape, top bar on inner portrait); elsewhere bar hidden, screens draw their own. Screens hand bar over via
  `LocalNativeChrome` (`:core:designsystem` `NativeChrome.kt`): `BackButton` registers itself, actions via
  `NativeItems(...)` (Back → prominent → groups → bottom), Compose key they replace skipped while native. New top-bar
  key on a screen: add its `NativeItems` entry. Bar crosses to Swift as JSON (`NativeShell.kt`).

**DI** = Koin. `editorModule` (`:editor`): library, styles, export, editor view models. `appModule`
(`app/shared/.../di/AppModule.kt`): phones' sources and screens. `PlatformModule.<platform>.kt` per target. Web's
module in `app/webApp/.../Main.kt`. Platform features = `expect`/`actual` in `:core:ui` (camera preview in
`:app:shared`, given to style editor as `LiveCamera`).

**Web editor** (`:app:webApp`) = shared `EditorApp` compiled to wasm, plus Kotlin/JS for browsers without WasmGC, in
`ComposeViewport`. Site ships both (`composeCompatibilityBrowserDistribution`; `onlyshots.js` loader picks).

- **Session only:** library = in-memory okio `FakeFileSystem` (`WebFiles`). Picked videos stay browser `File`s, kept
  by path in `media.js`.
- **`media.js`:** browser half. Playback via hidden `<video>` (frames copied into Compose so Haze blurs them),
  thumbnails, audio, luma, export decode/encode.
- **Frames:** go straight in and out of Skia's wasm memory (`WebMedia.kt`). Skiko byte-array copies too slow. Both
  builds run Skia in skiko's wasm (`skiko.mjs`), so same path.
- **Source sets:** web actuals in each module's `webMain` (js + wasmJs). Only `:video` has per-target code
  (`WebMemory`, `SkikoMemory`).
- **`js("…")`:** must be expression valid on both. No `??`, no `?.` (Kotlin/JS rejects).

## Screenshot tests

`screenshots/src/commonMain/.../OverlayPreviews.kt`, `ComponentPreviews.kt`, `ScreenPreviews.kt`: `@Preview` +
`@Mugshot`. KSP makes JVM tests.

- **Screens:** drawn from stateless bodies (`HomeBody`, `RecordBody`, `EditBody`, `OpenBody`, `StyleEditorBody`) with
  actions stand-in (`EditActions.None`, `RecordActions.None`) and still picture, inside `ProvideWindowLayout`, phone
  and tablet (`@ScreenShots` = `@MugshotDevices(PHONE, TABLET)`; `@Preview(widthDp)` ignored).
- **Goldens:** WebP in `screenshots/src/androidHostTest/snapshots/images/`. Fail → diff report
  `screenshots/build/reports/tests/mugshotTestAndroidMain/index.html`.
- **iPhone Duo goldens** (`app/iosApp/screenshots/duo`, PNG, half size): whole screen as the system shows it (side
  column, status, island), closed / open / half-open (inner display sideways). XCUITest `DuoScreenshotTests`, run per pose by
  `tools/ios-screenshots/run.sh` (folds simulator with mobilecli; XCTest can't move the hinge). Skipped without
  `DUO_POSE`, so smoke tests unaffected. Own simulator ("OnlyShots Duo screenshots"), never turned: turned the other
  way it lays out a pixel apart. Record after a wanted change on iOS: `run.sh record`.
- **Can't capture:** infinite animation (`RecordButton(Armed)`), `ModalBottomSheet`, screens that default
  `koinViewModel()` or call `expect` composables. Extract stateless body first.

## Screens

**Splash** (`design/splash/SPEC.md`): once per cold start, `SplashHost` over each app root draws the mark recording its
shot (`SplashMark`), then hands off into the first screen's `OnlyShotsMark(splashTarget = true)`. Android's system
splash icon is generated by `design/splash/avd.py`, iOS's launch image by `launch.py`; the web's splash is
`index.html`'s own.

**Navigation 3** everywhere: `NavDisplay` over `rememberNavBackStack(RouteSaving, …)` with saveable-state and
view-model entry decorators (each `koinViewModel()` lives as long as its entry). In `app/shared/.../App.kt` (phones)
and `editor/.../EditorApp.kt` (desktop, web). Web ties back stack to browser history
(`app/webApp/.../BrowserHistory.kt`, navigation3-browser): hierarchical on phone browsers, chronological on desktop.

- **Start** (`feature/home/.../StartScreen.kt`) offers two flows.
  - **Record live:** Home → Record → Edit (save + export) → Record.
  - **Edit by hand:** `OpenScreen` (`feature/open/`, also desktop/web start). Pick video + shot, both required. Shot
    from file, or (phones, desktop; not web) machine's recent shots over HTTP via `MachineShotsRoute`
    (`feature/home/MachineShotsScreen.kt`, `GaggiuinoHistory`), back through `NavResults.pickedShot` → Edit. Shot saved
    unplaced; editor lines it up. `EditRoute.manual` sends back/export to picking screen.
- **Home** (`feature/home/`): searches machines and scales while in front (stops via `LifecycleStartEffect`). User
  picks one source. UI rules = pure functions in `HomeState.kt` (`HomeStateTest`). Rows and cards in
  `core/designsystem/.../component/Home.kt`.
- **Edit** (`feature/edit/`): editor everywhere. Phone layout; on `Wide`, desk layout: picture + timeline beside panel
  that follows selection (graph: style, data, text; else video: shot, frame, export). Top bar: back, title, undo,
  redo. Export progress/result = full-screen `ExportOverlay`.
  - Undo/redo = `EditState`'s two stacks (Cmd/Ctrl+Z, +Shift redo).
  - "match by clock" lines shot up from video's `recordedAt`.
  - Export wording from host (`ExportWording`).
  - `EditTimeline` (Canvas; geometry and hit/snap rules in `TimelineMath`, tested). `OverlayEditLayer` (shared with
    Record; snap targets in `core/model/.../OverlaySnap.kt`). Sheets in `EditSheets.kt` on `EditorSheet`: plain
    layout, not `ModalBottomSheet`, so screenshot-testable.
