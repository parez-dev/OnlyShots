# Product page header and search results video

Apple added product page headers on 5 October 2026: a 21:9 slot above the product page taking an image or a video,
separate from screenshots and app previews. It is marketing art, it loops forever, and it is muted until someone
unmutes it — so unlike an app preview, it carries no device-capture rule.

Ours is the app's own output: a real shot with its real graph drawn over it, framed in the Instrument look. For an
app whose pitch is "export matches preview", nothing else is as strong.

- **Spec** (read 2026-10-29):
  <https://developer.apple.com/help/app-store-connect/reference/app-information/creative-assets-specifications/> —
  21:9 at 3840 × 1646 (or 16:9 at 5244 × 2950), 30 or 60 fps, 5 to 30 s, `.mov` / `.m4v` / `.mp4`.
## The crop

**The product page crops the header to the device's aspect.** On a phone only the middle ~81 % of the width
survives; a narrower layout keeps as little as 62 %, which is x 735 to 3105 of 3840. Anything near an edge is lost
— the first cut of this header lost the "F" of FILM and the graph's `weight flow` legend.

So the header's furniture is the logo alone, centred: the footage and the real graph carry it, and the words live
on the screenshots and the search banner, which nothing crops. Keep anything new inside that middle 2370 px.

The graph itself is burned into the master by the editor, so where it sits is a recording-time decision: placed
far right it will be clipped whatever the furniture does.

## The two slots

| Slot | Size | Cut from the master | Audio |
| --- | --- | --- | --- |
| `--slot header` | 3840 × 1646, 21:9 | its **height**, `--top` | kept: muted by default but a viewer can unmute |
| `--slot search` | 3240 × 2160, 3:2 | its **width**, `--left` | dropped: search audio can never be unmuted |

Apple also accepts 16:9 at 5244 × 2950 for both, which would mean upscaling a 4K master by 1.37; 3:2 at
3240 × 2160 cuts the width at native resolution instead, and sits inside the 1920 × 1280 to 3840 × 2560 range.
Each slot has its own furniture page: the header's copy block would not fit a slot seen at thumbnail size, and the
search banner needs its scrim from the left rather than the foot, the graph card reaching further down the frame
than the copy begins.

- **Build:** `python3 design/header/build.py <master.mp4> --in 12 --out 34` (ffmpeg, and Google Chrome or the one
  Playwright keeps for `tools/web-smoke`, which `design/store/render.py` finds). Writes
  `design/header/out/header-3840x1646.mp4`.

## The band

A 16:9 export is 2160 tall and the header keeps 1646 of it, so 514 px goes. `--top` says where that band sits.
Centred (the default) takes 257 off each end, which cuts the portafilter at the top of the frame; `--top 0` keeps
the top and drops the cup's base and the scale instead. Whichever you pick, the graph has to sit inside the band
with some air under it — pass `-Donlyshots.header.rect=x,y,w,h` to put it there.

What the current header uses:

```
./gradlew :video:desktopTest --tests '*HeaderMasterTest' \
  -Donlyshots.header.video=IMG_7462.MOV -Donlyshots.header.shot=shot.json \
  -Donlyshots.header.out=master.mp4 \
  -Donlyshots.header.shotStartUs=1000000 -Donlyshots.header.durationUs=19498333 \
  -Donlyshots.header.rect=0.52,0.28,0.42,0.34 \
  "-Donlyshots.header.title=Flimsy Light 0.3"
python3 design/header/build.py master.mp4 --in 0 --out 19.4 --top 0
```

`title` is worth setting: left alone the graph reads `"<profile> · <machine>"` (`overlayTitle`,
`core/model/.../Names.kt`), which overruns the header row and ellipsises.

## 1. Record the shot

In the app, with the machine and the scale connected.

- **Hold the phone in landscape before arming.** `formatThatFits` → `RecordViewModel.captureFormat` picks 9:16 or
  16:9 from how the screen stands, and turning it while armed re-arms. Landscape gives the 16:9 an ultra-wide
  header needs.
- **Keep the graph in the middle of the frame.** The master is 3840 × 2160 and the header is 3840 × 1646, so 257 px
  comes off the top and 257 off the bottom. Anything in the outer 12 % of the height is lost.
- Let the shot run to its stop weight, so the weight series climbs its full range.
- Audio is kept. The header is silent until someone unmutes, and a real pour is worth hearing.

One recording gives the video and its sidecar together, already aligned — no shot file to import, no "match by
clock".

## 2. Export the master

Either way, it goes through the app's own exporter, so the art is what the app makes.
`Quality.size(format)` (`core/model/.../Video.kt`) fixes output to one of eight sizes and none of them is 21:9,
which is why the master is 16:9 and the 21:9 framing happens in `build.py`.

**By hand**, when you have a recording the app made:

```
./gradlew :app:desktopApp:run
```

Open → the recording and its shot → format 16:9, quality 4K → check the graph sits in the middle band → export.

**Headless**, for a loose video plus a shot file — a camera roll clip and a visualizer.coffee shot, say:

```
./gradlew :video:desktopTest --tests '*HeaderMasterTest' \
  -Donlyshots.header.video=IMG_7462.MOV \
  -Donlyshots.header.shot=shot.json \
  -Donlyshots.header.out=master.mp4 \
  -Donlyshots.header.shotStartUs=1000000 \
  -Donlyshots.header.durationUs=19498333
```

`HeaderMasterTest` (`video/src/desktopTest/`) builds a `VideoItem` and runs `FfmpegVideoExporter`, in the
`liquidglass` style with the graph placed in the band the 21:9 crop keeps. It skips without
`onlyshots.header.video`, so `./gradlew :video:desktopTest` stays green. Any shot `ShotFiles.parse` reads works;
fetch a Visualizer one with `curl -s https://visualizer.coffee/api/shots/<id>?with_data=1 -o shot.json`.

### Lining the shot up

`shotStartUs` is where the shot begins in the video. The two clocks give it: the video's `creation_time` and the
shot's `start_time`, which is what `VideoItem.shotStartByClock()` does in the app.

```
ffprobe -v error -show_entries format_tags=creation_time -of default=nw=1 video.MOV
python3 -c "import json;print(json.load(open('shot.json'))['start_time'])"
```

Check it against the picture rather than trusting it: find the first shot sample where `espresso_weight` rises off
zero, add the offset, and the first liquid should reach the cup at that moment in the video.

## 3. Compose

`build.py` shoots `furniture.html` — the still Instrument layer, **the one page under `design/` whose alpha must
survive**, since ffmpeg lays it over the footage — then crops the master to 3840 × 1646, trims to `--in`/`--out`,
overlays the furniture and encodes H.264 at 30 fps with AAC stereo. It refuses a length outside Apple's 5–30 s and
asserts the written size, because a wrong one is otherwise caught only by App Store Connect refusing the upload.

**Looping.** The header cuts straight back to the start. Pick `--in` and `--out` so that cut is not jarring:
ending on a steady pour that resembles the opening frame beats ending mid-transition.

## Where the files live

`out/` and any master are git-ignored. The finished header is tens of megabytes, and App Store Connect's Asset
Library now stores approved assets and lets you swap them without shipping a new version — that is the artefact's
home. The repo keeps the source and the build.

## App preview — not built here

The other slot, 886 × 1920, 15–30 s, **is** device capture, so it is a screen recording off the iPhone rather than
anything this folder makes.
<https://developer.apple.com/help/app-store-connect/reference/app-preview-specifications/>: H.264 High ≤ 4.0 at
10–12 Mbps, max 30 fps, stereo 256 kbps AAC with **all tracks enabled** — a preview with no audio track at all is
a common rejection, so give a silent capture a silent stereo track. Up to three per size and language.

A capture comes off the phone at 1320 × 2868 and 60 fps, so it needs scaling to 886 × 1920 and halving to 30 fps.

Shot list, using the machine or Home's "try a sample shot" (the `SampleShots.zer0` replay through
`SourceHolder.useSample`, which needs no hardware and is the path App Review uses):

| | Beat | Seconds |
| --- | --- | --- |
| 1 | Home, machine and scale found | 3 |
| 2 | Record armed, the shot starting itself off the brew switch | 6 |
| 3 | The graph drawn live over the pour, dragged to another corner | 7 |
| 4 | Edit: a style picked, the overlay changing with it | 5 |
| 5 | Export, and the finished video playing | 4 |
