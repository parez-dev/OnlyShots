# OnlyShots: developer guide

How OnlyShots is built, tested and released. For what the app does, the machines and scales it reads, and how to add
a new machine, see the [README](../README.md).

- **Phones:** Android and iOS. Phone, tablet, foldable. Record and edit.
- **Desktop and web:** editor only. Pick video, pick shot, export.
- **Rule book:** Figma. Spec card acceptance criteria = test cases.

## Quick start

No setup. No secrets. Clone, build.

```
./gradlew :app:androidApp:assembleDebug                      # Android app
./gradlew :app:desktopApp:run                                # desktop editor
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun            # web editor, http://localhost:8080
cd app/iosApp && xcodegen generate                           # iOS: then build scheme iosApp in Xcode
```

Every build starts disconnected: "record without data" records without data. Home's "no machine? try a sample shot"
link replays the Zer0 sample shot (`FakeShotSource`) for one take, in any build; tap **simulate shot** while armed to
pull it by hand.

## Layout

Kotlin's [recommended structure](https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html),
client-server style. Apps live in `app/`. Shared libraries live at root.

| Module | What |
| --- | --- |
| `:app:shared` | Phones' app (Start → Home → Record → Edit), Koin (`initKoin`), platform modules, iOS entry (`MainViewController`, Swift export module `OnlyShotsApp`) |
| `:app:androidApp` | Android app: `MainActivity`, manifest, icons, release build |
| `app/iosApp` | Xcode project. SwiftUI host. Links `:app:shared` |
| `:app:desktopApp` | Desktop editor: `main()`, packaging |
| `:app:webApp` | Web editor: `ComposeViewport`, web Koin, `media.js` (files, play, decode, encode) |
| `:editor` | `EditorApp` (Open → Edit → style editor) + `editorModule`. What desktop and web run |
| `:feature:home` | Start, Home (find machine and scale), machine log, machine shot picker |
| `:feature:record` | Record screen, `RecordSession` state machine |
| `:feature:edit` | Editor: timeline, sheets, graph toolbar, reframe, undo/redo, export manager |
| `:feature:open` | Editor start: pick video + shot (file, or from machine) |
| `:feature:style` | Style picker, style library, style editor |
| `:core:ui` | Shared screen stuff: adaptive layout, routes, Navigation 3 helpers, platform `expect` composables |
| `:core:designsystem` | Instrument tokens (ink, bone, signal, hairlines, square), Plex type, components, splash mark |
| `:core:resources` | All fonts, icons, illustrations, strings. `UiText` |
| `:core:model` | Shot, sample, overlay settings, video, sidecar schema, stop rules, Zer0 sample |
| `:overlay` | Graph styles + pure scene builder. No Compose |
| `:overlay:compose` | Scene drawn with Compose: `drawOverlay`, `ShotOverlay`, `OverlayFrameRenderer` |
| `:sources` | `ShotSource`. Network machines (WebSocket), Bluetooth scales (Kable), scan, fake replay |
| `:video` | Exporters (Media3, AVFoundation, FFmpeg, browser), probes, players, pump-start finder |
| `:storage` | Sidecars, settings, share temp files |
| `:camera` | `CameraController`, pre-roll buffer |

Convention plugins in `build-logic/`: `onlyshots.kmp.library`, `onlyshots.kmp.wasm` (wasm + js),
`onlyshots.compose`, `onlyshots.feature`, `onlyshots.versioning`.

## How it works

**One scene, many painters.** `buildOverlayScene(...)` is pure. Shot + time + settings + style in. Flat list of
rects, lines, areas, text out. Sized from 390-wide frame. So export = exact scaled copy of preview. `drawOverlay`
draws it everywhere: record, editor, screenshots, export. New visual thing? Put it in scene builder.

- **Curves:** smoothed with monotone cubic (`scene/Smoothing.kt`). No overshoot. Flat stays flat. Idea from reading
  HDCharts (MIT). No code copied.
- **Words in video:** come as `OverlayLabels`. Screens and exporters resolve same strings. Preview = export.

**Styles are data.** Token set in `core/model/.../style/GraphStyle.kt`. Built-ins: Instrument (the app's own look, in
Plex Mono), Gaggiuino, Metro, Aero, Minimal, Liquid Glass. Each sets its text in Open Sans or Plex Mono
(`TextColors.font`); `OverlayFonts` carries both to every painter. Old ids (`gweb`, `tiles`, …) map to closest. User makes own in style editor. User styles live in
`settings.json`. Video keeps copy in its sidecar.

- **Blur and glass:** scene marks blur area with `BackdropPrim`. Preview: Haze draws it. Export: each exporter blurs
  same rects. Glass uses Haze's vendored shader (Apache-2.0), SkSL for Skia, GLSL for Android and WebGL.
  `GlassMapping.kt` maps once for both sides.
- **Adapt to video:** style can flip to dark set over bright frames. `LumaTrack` measures. `Readability.kt` checks
  4.5 : 1 and editor offers "fix it for me".
- **Rebuild test:** `BuiltInsAreReplicableTest` rebuilds every built-in with editor's own edits (`StyleEdits.kt`).
  New style field needs edit + control, or test fail.

**Formats.** 9:16 (default), 4:5, 1:1, 16:9. Export 1080 (or 2160) on short side. Graph sized to short side.
Record screen picks no format: camera fills screen, films its whole frame as held (9:16 upright, 16:9 on its side).
Screen locks only while take runs. Editor's **⤢ frame** picks format and moves crop.

**Live data.** Every machine or scale = `ShotSource`. Network machine = `NetworkMachine` in
`NetworkMachines.all`. Scale = pure `BleProtocol` over Kable. `SourceHolder` swaps live source under every screen.

**Recording.** Runs from arm. Pre-roll is in file. `trimInUs` cuts idle head. While armed, file restarts every 15 s.
Late shot gets old tail joined in front (`Mp4Join` on Android; on iOS old writer keeps going).

**Storage.** Sidecar JSON per video, beside the video = only truth. No database: nothing lists the library.

**Layout.** `windowLayout(...)` picks `Phone`, `Wide`, `Stacked`, `Tiny`. Every adaptive branch goes through it.
Foldables follow "OnlyShots Foldables" canvas: nothing on fold; picture one pane, controls other. iPhone Duo follows
Figma "OnlyShots · iPhone Duo" (Apple HIG "Designing for iPhone Duo"): system bars, not Compose ones, via the native
shell (`NativeShell.swift`, `LocalNativeChrome`); status, Dynamic Island and toolbar share a side column except on
the inner display upright. Check on the iPhone Duo simulator (Xcode 27.1): closed, open, half-open, both orientations.

**Look.** "Instrument" (canvas "OnlyShots · Instrument"): ink `#0B0B0B`, bone `#E8E6E1` text, one signal `#FF5A1F`
accent, `#3A3A38` hairlines, square corners. IBM Plex Mono SemiBold names and counts (labels drawn in capitals with
`String.caps()`, strings stay sentence case), Plex Sans says sentences. Every value is a token in
`core/designsystem` (`Tokens.kt`, `Type.kt`). Logo: the Signal tile (`design/logo/`), splash: the tile plotting the shot
(`design/splash/SPEC.md`).

## Tests

```
./gradlew testAndroidHostTest                    # shared tests, JVM (CI runs this)
./gradlew :screenshots:verifyMugshot             # screenshots, JVM, no device
./gradlew :screenshots:recordMugshot             # re-record after wanted visual change
./gradlew iosSimulatorArm64Test                  # shared tests + AVFoundation export, simulator
./gradlew :video:connectedAndroidDeviceTest      # Media3 export alignment, needs device
./gradlew :video:desktopTest                     # FFmpeg export alignment, JVM
./gradlew ktfmtCheck                             # format gate. Fix with ktfmtFormat
./gradlew :uitests:desktopTest                   # UI flows: phone app and editor, every layout, real FFmpeg
./gradlew :app:desktopApp:test                   # desktop app starts on Open, its own Koin
./gradlew :app:uitests:connectedAndroidTest      # Android smoke: record, export, rotate (device or emulator)
(cd app/iosApp && xcodegen generate && xcodebuild test -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17')
(cd tools/web-smoke && npm ci && npx playwright install chromium && node smoke.mjs)  # after the web build
```

- **UI tests:** find controls by `app.onlyshots.TestTags` (`Modifier.testTag`), never by words or looks, so a
  restyle can't break them. Android sees them as resource ids (`testTagsAsResourceIds()` on the activity root and on
  each dialog, sheet and menu window), iOS as accessibility identifiers. `:uitests` runs `OnlyShotsApp` and `EditorApp`
  in desktop windows at phone, tablet, book-fold, tabletop, flex and cover sizes (`Devices.kt`), pickers answered by
  `LocalPickerOverride`, postures forced by `LocalWindowLayoutOverride`.

- **Screenshots:** `@Preview` + `@Mugshot` in `screenshots/`. Layoutlib renders on JVM. Goldens are WebP in
  `screenshots/src/androidHostTest/snapshots/images/`. Fail = HTML diff report in
  `screenshots/build/reports/tests/mugshotTestAndroidMain/`.
- **Can't capture:** infinite animation (`RecordButton(Armed)`), `ModalBottomSheet`, screen wrappers with
  `koinViewModel()`. Preview stateless body instead (`HomeBody`, `RecordBody`, `EditBody`, …). Haze blur not drawn by
  layoutlib; device test checks it.
- **Export alignment:** test clip carries frame number as barcode. `Media3ExportTest`, `AvFoundationExportTest`,
  `FfmpegExportTest` decode export, check overlay within one frame.

## Build notes

- **Android release:** `./gradlew :app:androidApp:assembleRelease`. R8 on. Signed only when `ONLYSHOTS_KEYSTORE_FILE`,
  `ONLYSHOTS_KEYSTORE_PASSWORD`, `ONLYSHOTS_KEY_ALIAS`, `ONLYSHOTS_KEY_PASSWORD` set (release.yml does). Else unsigned.
  Sign ad hoc to try:
  ```
  apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android \
    --ks-key-alias androiddebugkey --out /tmp/onlyshots.apk \
    app/androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk
  ```
  R8 breaks reflection quietly. Walk six paths by hand: record and save, export and share, connect over HTTP, BLE
  scan, settings change that lives past force-stop.
- **AGP 9:** library test sets are `androidHostTest` / `androidDeviceTest`. `:app:androidApp` is plain
  `com.android.application` on built-in Kotlin.
- **Resources gap:** plugin no package library's Compose resources. `app/androidApp/build.gradle.kts` copies
  `:core:resources` into APK assets. Screenshots get same files on classpath.
- **iOS:** `app/iosApp/project.yml` makes Xcode project (`xcodegen`). Imports `:app:shared` as Swift export module
  `OnlyShotsApp`, built by its pre-build phase (`embedSwiftExportForXcode`).
- **Secrets:** none in repo. CI secrets: `VERCEL_TOKEN` (org secret, web deploy); Play and App Store release (repo secrets, see
  below).

## Version and release

One version: `onlyshots.version` in `gradle.properties`. Checked as semver by
[kotlin-semver](https://github.com/z4kn4fein/kotlin-semver). Android, iOS, desktop and in-app "OnlyShots x.y.z" all
read it. Build number = `major × 10000 + minor × 100 + patch` (1.2.3 → 10203).

```
./gradlew bumpVersion -Ppart=minor     # 1.0.0 → 1.1.0, iOS files too
./gradlew bumpVersion -Pto=2.0.0       # exact; must be later
./gradlew checkAppVersion              # CI: iOS files match?
```

macOS no take 0.x version. So start 1.0.0.

Release = tag:

```
./gradlew bumpVersion -Ppart=minor && git commit -am "OnlyShots 1.1.0"
git tag v1.1.0 && git push origin main v1.1.0
```

`.github/workflows/release.yml` checks tag = version. Uploads signed Android bundle to Play internal testing. Builds
desktop DMG, MSI, DEB on each OS. Publishes GitHub Release with notes (desktop only; bundle stays a run artifact).

- **Web:** CI deploys from `main`. Live before tag.
- **Desktop:** unsigned. macOS: right-click › Open first time. Windows SmartScreen warns.
- **Android:** `bundleRelease` signed with upload key → Play internal track (`dev.parez.onlyshots`). Promote in Play
  Console. Repo secrets: `ANDROID_KEYSTORE_BASE64` (`base64 -i upload.jks`), `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON` (service account with Android Publisher
  API, invited in Play Console with release rights). Play API rejects app with no bundle yet: first one by hand from
  run's `android-release` artifact. Same version twice fails: bump.
- **iOS:** `release.yml`'s `ios` job archives, exports and uploads to App Store Connect, signing as an App Store
  Connect API key rather than a keychain certificate: `-allowProvisioningUpdates` with `-authenticationKey*` makes the
  distribution certificate and the App Store profile itself. Repo secrets: `APPSTORE_API_KEY_P8` (`base64 -i
  AuthKey_XXXX.p8`), `APPSTORE_API_KEY_ID`, `APPSTORE_API_ISSUER_ID` (App Store Connect › Users and Access ›
  Integrations, App Manager or better), `APPLE_TEAM_ID` (Membership page). The app record must exist for
  `dev.parez.onlyshots` before a build is accepted, and the same build number twice fails: bump. First one can go by
  hand from the run's `ios-release` artifact.

## Desktop editor

`./gradlew :app:desktopApp:run`. Pick video (or drop it), pick shot (file, or straight from Gaggiuino). Place graph.
Export. Library in `~/.onlyshots-desktop`. Exports in `~/Movies/OnlyShots`.

- **FFmpeg** via JavaCV (FFmpeg 8.1, GPL build for libx264). Play, probe, export.
- **One OS per build:** only host's FFmpeg natives (`desktopNatives` in `video/build.gradle.kts`).
  `packageDistributionForCurrentOS` packages for that OS.
- **Same frames as phones:** `SkiaFrameDrawer`, same Skia code as iOS.
- **Trimmed runtime:** packaged app has only listed JDK modules (`modules(...)`). New JDK API? Run
  `suggestRuntimeModules`, launch packaged app.

## Web editor

Same Compose editor, compiled to wasm. Kotlin/JS too, for browsers without WasmGC. No recording.

```
./gradlew :app:webApp:composeCompatibilityBrowserDistribution   # wasm + JS, app/webApp/build/dist/composeWebCompatibility
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun               # wasm, http://localhost:8080
./gradlew :app:webApp:jsBrowserDevelopmentRun                   # JS, same port
tools/deploy-web.sh                                             # build here, publish onlyshots.vercel.app
```

- **Deploy:** Vercel builds nothing (Ignored Build Step `exit 0`). CI or `deploy-web.sh` uploads static files.
- **Session only:** in-memory files (`WebFiles`). Reload = empty. Picked video stays browser `File`.
- **`media.js`:** browser half. File dialog, drop, play, thumbnails, sound, luma, export decode/encode. Uses
  [Mediabunny](https://mediabunny.dev) (MPL-2.0) and WebCodecs.
- **Export:** 1080 × 1920, silent, on GPU through own WebGL. Needs `VideoEncoder`: Chrome, Edge, Safari 16.4+.
  Frames go straight into Skia memory. Nothing uploaded.
- **`js("…")` code:** must be expression both targets take. No `??`, no `?.`.

**App links.** `onlyshots.vercel.app/open` opens app: Android App Links (`autoVerify` on `MainActivity`), iOS
Universal Links (`app/iosApp/iosApp/iosApp.entitlements`). No app? Phone goes to store, rest to editor. Phone browser
shows store badge (`app/webApp/.../AppStores.kt`). Apps not published yet, so placeholders:

- **App Store id:** `AppStores.APP_STORE_ID`, `APP_STORE_ID` in `open/index.html`, `apple-itunes-app` tag in
  `index.html`.
- **Apple Team ID:** `appIDs` in `.well-known/apple-app-site-association`.

`.well-known/assetlinks.json` holds two SHA-256s: Play's app signing key (store installs) and the upload key (CI's
bundle, sideloaded). Rotate either key? Update it there.

Check after deploy: `adb shell pm get-app-links dev.parez.onlyshots`, Apple's AASA validator.

**Shot files.** "from a file" goes through `ShotFiles.parse`, which takes the file's bytes and tells five shapes
apart. A GaggiMate `.slog` is binary and known by its first four bytes; the rest are JSON, told apart by shape:

- Gaggiuino `.shot` JSON (`datapoints`): what `GET /api/shots/{id}` and baristagiuino give. `GaggiuinoShotFile`. Older
  firmware: parallel arrays, time in deciseconds, values ×10. Newer: one object per reading, real units, ms.
- GaggiMate `.slog` (magic `SHOT`): the machine's own binary log, also what `GET /api/history/<id>.slog` serves.
  `GaggiMateShotLog`. Little-endian, 128- or 512-byte header then fixed-width samples; `fieldsMask` says which fields
  a sample carries and the record size tells a v6 index stamp from a v6 millisecond one. Same file parses the `SIDX`
  index behind `recent.bin`.
- GaggiMate export JSON (`samples` of `{t, ct, cp, …}`): what the machine's Shot Analyzer hands out.
  `GaggiMateShotFile`. Real units, time in ms.
- visualizer.coffee's normalized shot (`timeframe` + `data.espresso_*`): any machine's, as the visualizer kept it (it
  keeps no upload). `VisualizerShot`. A Decent's values arrive as strings, its missing goals as -1; phases are where
  `espresso_state_change` flips sign.
- Beanconqueror's brew (`brewFlow`, bare or under the visualizer's `brewdata`). `BeanconquerorShot`. One list per
  sensor at its own rate, lined up by wall-clock `timestamp` (nearest within 250 ms, else left out).

Missing series left out, not guessed. All shots read from the visualizer or Beanconqueror are `SourceKind.VISUALIZER`.

**Caching the web build.** `app/webApp/vercel.json` caches a wasm file for a year as `immutable`, and that rule has
to match **content-hashed names only** (`/:hash([0-9a-f]{8,}).wasm`). The wasm build's bundles carry their hash, so a
deploy writes a new name and the stale one is never asked for. `skiko.wasm`, which the Kotlin/JS build loads through
`skiko.mjs`, keeps its name forever: caching it as immutable pairs a year-old wasm with a freshly revalidated loader,
Skia never starts and the page is blank for exactly the people who visited before. Everything unmatched falls back to
Vercel's `max-age=0, must-revalidate`, which is what `skiko.mjs` and both JS bundles already get.

**Visualizer.** `:visualizer` fetches a shared shot: a share code (four letters from the shot page's Share, valid about an
hour) via `GET /api/shots/shared?code=X&with_data=1` (without `with_data` no readings), or a link / id via
`GET /api/shots/{id}`. No account. CORS allows any origin, so the web editor fetches too (Ktor `Js` engine). 429 past 50
requests a minute. Its own `HttpClient`, never the machine one. Bound in `editorModule`; UI tests bind a fake
(`SharedOnVisualizer` in `uitests/.../Harness.kt`).

## Status

All seven milestones done: spike, foundations, record, editor, share, more sources, adaptive + a11y + store builds.

- **Not checked on real hardware:** wide layouts on tablet, phone landscape, desktop window. Flip cover screen
  (`Tiny`): only by resizing. iPhone Duo tabletop and camera.
- **Gaggiuino:** binary firmware seen on real machine (Oct 18, 2026). Scan and identify tested on fakes, not real
  machine on phone.
- **Gaggiuino temperature, on purpose:** binary firmware pushes two streams. `d_sensor_snap` carries the machine's
  thermocouple, which climbs through an extraction; `d_shot_snap` reports the setpoint as its `temperature`, which is
  what the machine's own charts and its shot files show as a flat line (in a shot file `temperature` equals
  `targetTemperature` on every sample, the machine having no temperature profiling). The graph keeps the
  thermocouple, because it is the measurement; `d_shot_snap` is read only for the targets, which the sensor stream
  does not carry at all. Expect the curve to disagree with the machine's web UI, and that to be right.
- **GaggiMate:** written from upstream source (master `b78beee`, Oct 18, 2026) and tested against captured frame
  shapes and hand-built `.slog` bytes. Not yet run against a real machine, and the pressure gate on `cp` is untested
  on a Standard.
- **Home spec not built yet:** machine + scale together, remembered machines offline, rename/forget, live reading
  line on card, pressure sensors.
- **Android 14 and older:** export service runs as `dataSync` (`mediaProcessing` from Android 15 only).

**Play Console quality cards (1.0.0, assessed Oct 27, 2026).** Both are advisory, neither blocks a release, and in
both cases the API Play names is one the app never calls.

- **"Edge-to-edge may not display for all users".** The display is right: every screen root applies
  `safeDrawingPadding()`, no theme sets a bar colour, and there is no deprecated window call in app code. The flagged
  `Window.setStatusBarColor` / `setNavigationBarColor` are inside `androidx.activity`'s `enableEdgeToEdge`, which
  still sets them in `EdgeToEdgeApi26`, `Api29` and `Api35`. `MainActivity` now calls it only below API 35, where
  those setters are not deprecated and are what makes the bars transparent at all; from 35 the window is edge to edge
  by itself and only the icon colours are set, through `WindowCompat`. **The card is expected to stay** while
  `minSdk = 26`: Play scans the merged DEX, the legacy branch keeps those androidx classes, and clearing it entirely
  would mean giving up edge to edge on Android 8 to 14. The split was checked on an API 35 emulator against the
  previous code, in both system themes, by reading `vsysui` out of `dumpsys window`: identical either way, so it is
  a deprecation change and nothing else.
- **Pre-existing, found while checking the above:** on API 35 under a *light* system theme the window comes up with
  `vsysui=LIGHT_NAVIGATION_BAR`, i.e. dark navigation icons, although the app is `#0B0B0B` whatever the system does
  and `MainActivity` asks for light ones. Same before and after the change, so `enableEdgeToEdge`'s
  `SystemBarStyle.dark` is not winning on 35. Not chased yet; it is a thin gesture handle on most devices.
- **"Improve your app's performance with bitmap downsampling".** The app calls `BitmapFactory` nowhere; the card is
  most likely Compose's `decodeToImageBitmap` (a `BitmapFactory.decodeByteArray` underneath) or androidx.core. The
  real problem it pointed at was ours though, and is fixed: `MediaMetadataRetriever.getFrameAtTime` decodes at the
  source's own resolution, 33 MB a frame at 4 K, and `lumaAt` was doing that every 250 ms of a take to read twelve
  pixels. Both Android call sites now go through `scaledFrameAt` (`video/.../ScaledFrame.kt`), which hands the size
  to the decoder as iOS and the web already did. Whether the card clears is only visible after an upload.

**App Store:**

- `PrivacyInfo.xcprivacy` lists API reasons (`C617.1`, `E174.1`, `CA92.1`). Check with `nm -u` after new deps.
- Privacy and support: https://onlyshots.vercel.app/privacy.html.
- `ITSAppUsesNonExemptEncryption` = false.
- Reviewers have no machine: tell them about "try a sample shot" and **simulate shot**.
- **iPhone Duo:** screenshots needed from April 2027. Scene manifest in `Info.plist` must stay (else unfold kicks
  app out). Fold comes from Swift `ComposeHostController` (`NativeShell.swift`).
- **Media.** Screenshots are built by `design/store/` (`appstore/`, its README has the slot table). The product
  page header, a 21:9 video slot Apple added on 5 October 2026, is built by `design/header/` from a real recording
  exported at 16:9 / 4K; it is marketing art with no device-capture rule, it loops, and it is muted by default.
  The app preview (886 x 1920, 15-30 s) **is** device capture, so it is a screen recording off the phone;
  `design/header/README.md` holds its spec and shot list. Sizes come from Apple's own pages, which the
  third-party guides disagree with.

## Open questions

- **Gaggiuino history:** `/api/shots/latest` shape not seen from machine. `GaggiuinoHistory` takes
  `[{"lastShotId": "121"}]`, `{"lastShotId": 121}`, or bare number. Check on machine.
- **Gaggiuino mDNS:** does firmware advertise service, or only `gaggiuino.local`? Filter (`name` has "gaggiuino") is
  guess. Scan works either way.
- **Gaggiuino brewing frames:** what binary firmware sends only while brewing. Can history come over socket.
- **Gaggiuino icon:** maintainers' OK.
- **Min iOS:** 18 (Swift export needs it). minSdk 26.
- **Toolchain:** AGP 9.3.2, Gradle 9.6.1, Kotlin 2.5.0-Beta2, Compose 1.12.1.
