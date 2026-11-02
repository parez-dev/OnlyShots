#!/usr/bin/env python3
"""Builds the Play Store art in play/ from the screens render.py made.

Same layouts as the Figma file (README.md has the link): ink ground with a hairline grid, an orange label, a
Plex Mono headline and a Plex Sans line over the screen in a hairline device with signal-orange corner ticks.
Each frame is laid out at 1x and shot with headless Chrome at the scale that gives the store size.

    python3 design/store/render.py && python3 design/store/compose.py   # needs ImageMagick too
"""

import html
import pathlib
import typing
import shutil
import subprocess
import tempfile

from render import CHROMES

HERE = pathlib.Path(__file__).parent
SCREENS = HERE / "screens"
PLAY = HERE / "play"
APPSTORE_DIR = HERE / "appstore"
FRAMES = HERE / "frames"
# The iPhone Duo simulator captures (tools/ios-screenshots/capture.sh); git-ignored, so the Duo slots are
# skipped when they are not there.
DUO_SHOTS = HERE.parent.parent / "app" / "iosApp" / "build" / "duo-screenshots"
LOGO = HERE.parent / "logo" / "kit"

INK, BONE, SIGNAL, LINE, MUTE, BLUE = "#0B0B0B", "#E8E6E1", "#FF5A1F", "#3A3A38", "#A3A19B", "#7FA7B8"

PHONE = [
    ("Main", "01 / START", "FILM THE SHOT.\nKEEP THE DATA.", "Film espresso with its live graph from your machine and scale."),
    ("Home-Found", "02 / CONNECT", "FINDS YOUR\nMACHINE AND SCALE", "Gaggiuino and GaggiMate over Wi-Fi, scales over Bluetooth."),
    ("Rec-Recording", "03 / RECORD", "STARTS WHEN\nTHE SHOT DOES", "Recording follows the brew switch."),
    ("Rec-Graph", "04 / PLACE", "THE GRAPH WHERE\nYOU WANT IT", "Drag it, resize it, pick what it shows."),
    ("Edit-Idle", "05 / EDIT", "LINE IT UP AFTER", "Any video, any shot file. Trim and export."),
    ("Rec-Style", "06 / STYLE", "SIX STYLES,\nOR YOUR OWN", "Every export matches the preview."),
]
TABLET = [
    ("Rec-Recording", "01 / RECORD", "STARTS WHEN\nTHE SHOT DOES", "Recording follows the brew switch, with the graph drawn live."),
    ("Edit-Idle", "02 / EDIT", "LINE IT UP AFTER", "Any video, any shot file. Trim, frame and export."),
    ("Home-Found", "03 / CONNECT", "FINDS YOUR\nMACHINE AND SCALE", "Gaggiuino and GaggiMate over Wi-Fi, scales over Bluetooth."),
    ("Rec-Style", "04 / STYLE", "SIX STYLES,\nOR YOUR OWN", "Every export matches the preview."),
]

# The device frames of Apple's App Store template, exported from the Figma file as transparent art
# (design/store/frames). `w`/`h` are the art's own pixels, `screen` the cut-out inside it; `over` marks the one
# frame whose screen is not cut out, so the picture is drawn on top of it instead.
class Device(typing.NamedTuple):
    art: str
    w: int
    h: int
    screen: tuple[int, int, int, int]
    over: bool = False
    #: Corner radius of the screen in the art's own pixels; only needed when `over` leaves it unmasked.
    radius: float = 0


IPHONE = Device("iphone-18-pro-black.png", 1319, 2748, (57, 63, 1206, 2622), over=True, radius=160)
IPAD_P = Device("ipad-portrait.png", 1880, 2640, (106, 110, 1668, 2420))
IPAD_L = Device("ipad-landscape.png", 2640, 1880, (110, 106, 2420, 1668))
DUO_OUTER = Device("duo-outer.png", 1574, 2194, (88, 80, 1398, 2034))
DUO_INNER = Device("duo-inner-landscape.png", 3093, 2247, (120, 120, 2853, 2007))

# The Duo's side column is iOS's own bar on a hinged device, so it exists in no artboard: these frames take the
# simulator captures instead (tools/ios-screenshots/capture.sh), named <pose>-<way up>-<screen>.png.
DUO = [
    ("home", "01 / CONNECT", "FINDS YOUR\nMACHINE AND SCALE", "Gaggiuino and GaggiMate over Wi-Fi, scales over Bluetooth."),
    ("start", "02 / START", "FILM THE SHOT.\nKEEP THE DATA.", "Film espresso with its live graph from your machine and scale."),
    ("open", "03 / EDIT", "LINE IT UP AFTER", "Any video, any shot file. Trim and export."),
]

# The App Store slots. App Store Connect offers three screenshot tabs for this app — "iPhone with Dynamic Island
# (medium display)", "iPhone Duo" and "iPad 13-inch" — and each takes up to ten pictures in one sequence, whatever
# their shape: the Duo tab's own hint lists 1398 x 2034 and 2853 x 2007 side by side, and the iPad tab takes both
# orientations. So these rows are grouped by tab, not by device shape, through `into`: a folder and the number the
# row starts at. There is no 11-inch tab, so no 11-inch set is made.
#
# Sizes are the ones the template's artwork is drawn for, all on Apple's accepted list. Apple scales the larger
# iPhone slots down from the medium one, so no 6.9-inch set is made either.
#
#   (into, frames, how to find the screen, frame w, h, grid cell, scale, copy(x, y, w, hs), device x/y/w)
FRAMED = [
    (("iphone", 1), PHONE, lambda n: SCREENS / f"{n}.png", 402, 874, 26, 3, (27, 36, 336, 28), (60, 232, 282)),
    (("iphone-duo", 1), DUO, lambda n: DUO_SHOTS / f"closed-portrait-{n}.png", 466, 678, 31, 3, (31, 41, 388, 32), (106, 262, 253)),
    (("iphone-duo", 4), DUO, lambda n: DUO_SHOTS / f"open-landscape-{n}.png", 951, 669, 34, 3, (46, 60, 392, 34), (470, 167, 460)),
    (("ipad-13", 1), TABLET, lambda n: SCREENS / f"{n}-M.png", 1024, 1366, 55, 2, (68, 82, 888, 62), (192, 433, 640)),
    (("ipad-13", 5), TABLET, lambda n: SCREENS / f"{n}-B.png", 1366, 1024, 34, 2, (56, 184, 352, 44), (536, 241, 760)),
]

DEVICE_OF = {
    ("iphone", 1): IPHONE,
    ("iphone-duo", 1): DUO_OUTER,
    ("iphone-duo", 4): DUO_INNER,
    ("ipad-13", 1): IPAD_P,
    ("ipad-13", 5): IPAD_L,
}


HEAD = f"""<!doctype html><meta charset="utf-8">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;600&family=IBM+Plex+Sans:wght@400&display=swap">
<style>
body{{margin:0;background:{INK}}}
.f{{position:relative;overflow:hidden;background:{INK}}}
.grid{{position:absolute;inset:0;background-image:linear-gradient(rgba(232,230,225,.05) 1px,transparent 1px),
  linear-gradient(90deg,rgba(232,230,225,.05) 1px,transparent 1px)}}
.copy{{position:absolute;display:flex;flex-direction:column}}
.label{{font:600 var(--l)/1.3 'IBM Plex Mono';letter-spacing:.08em;color:{SIGNAL}}}
.head{{font:600 var(--h)/1.18 'IBM Plex Mono';letter-spacing:-.02em;color:{BONE};white-space:pre-line}}
.sub{{font:400 var(--s)/1.4 'IBM Plex Sans';color:{MUTE}}}
.dev{{position:absolute;box-sizing:border-box;background:#111110;border:1px solid {LINE}}}
.dev img{{position:absolute;object-fit:cover;display:block}}
.tick{{position:absolute;background:{SIGNAL}}}
</style>"""


def ticks(w: int, h: int, t: int) -> str:
    o = t // 2
    spots = [(-o, -o), (w - o, -o), (-o, h - o), (w - o, h - o)]
    return "".join(f'<i class="tick" style="left:{x}px;top:{y}px;width:{t}px;height:{t}px"></i>' for x, y in spots)


def device(x: int, y: int, sw: int, sh: int, pad: int, tick: int, src: pathlib.Path) -> str:
    w, h = sw + 2 * pad, sh + 2 * pad
    return (
        f'<div class="dev" style="left:{x}px;top:{y}px;width:{w}px;height:{h}px">'
        f'<img src="{src.as_uri()}" style="left:{pad - 1}px;top:{pad - 1}px;width:{sw}px;height:{sh}px">'
        f"{ticks(w - 2, h - 2, tick)}</div>"
    )


def framed(x: float, y: float, w: float, shot: pathlib.Path, dev: "Device") -> str:
    """A screenshot inside one of the template's device frames (design/store/frames), drawn `w` wide at 1x.

    The bezels come out of the App Store template with their screens cut out, so the picture goes under the frame
    and shows through. The iPhone's is the whole component, placeholder screen and all, so there the picture goes
    over it instead; `over` says which. A screen that is not the frame's shape is cover-cropped, as the editor
    crops a clip into a format.
    """
    k = w / dev.w
    sx, sy, sw, sh = (v * k for v in dev.screen)
    # Ink under the picture and `contain` over it: a screen that is not the frame's shape then letterboxes onto the
    # app's own ground instead of losing its edges to a crop, which on an ink UI cannot be seen.
    screen = (
        f'<div style="position:absolute;left:{sx:.1f}px;top:{sy:.1f}px;width:{sw:.1f}px;height:{sh:.1f}px;'
        f'background:{INK};overflow:hidden;border-radius:{dev.radius * k:.1f}px">'
        f'<img src="{shot.as_uri()}" style="position:absolute;inset:0;width:100%;height:100%;object-fit:contain">'
        f"</div>"
    )
    bezel = f'<img src="{(FRAMES / dev.art).as_uri()}" style="position:absolute;left:0;top:0;width:{w:.1f}px">'
    inner = bezel + screen if dev.over else screen + bezel
    return f'<div style="position:absolute;left:{x:.1f}px;top:{y:.1f}px;width:{w:.1f}px">{inner}</div>'


def copy(x: int, y: int, w: int, label: str | None, head: str, sub: str, hs: int) -> str:
    gap = round(hs * 0.45)
    lab = f'<div class="label">{html.escape(label)}</div>' if label else ""
    return (
        f'<div class="copy" style="left:{x}px;top:{y}px;width:{w}px;gap:{gap}px;'
        f'--l:{round(hs * 0.44)}px;--h:{hs}px;--s:{round(hs * 0.56)}px">'
        f'{lab}<div class="head">{html.escape(head)}</div><div class="sub">{html.escape(sub)}</div></div>'
    )


def frame(w: int, h: int, cell: int, body: str) -> str:
    return f'{HEAD}<div class="f" style="width:{w}px;height:{h}px"><div class="grid" style="background-size:{cell}px {cell}px"></div>{body}</div>'


def feature() -> str:
    def p(d: str) -> str:  # the Start card's 300x120 trace space, mapped into the 388x200 plot
        out, nums = [], d.replace("M", " M ").replace("C", " C ").replace("L", " L ").split()
        i, axis = 0, 0
        for tok in nums:
            if tok in "MCL":
                out.append(tok)
                continue
            v = float(tok)
            out.append(f"{16 + v * 388 / 300:.1f}" if axis == 0 else f"{56 + v * 200 / 120:.1f}")
            axis ^= 1
            i += 1
        return " ".join(out)

    grid = "".join(f"M{p(f'M0 {y} L300 {y}')[2:]} " for y in (24, 60, 96))
    grid += "".join(f"M{p(f'M{x} 0 L{x} 120')[2:]} " for x in (75, 150, 225))
    traces = [
        ("M0 118 C20 118 30 30 50 24 L180 24 C220 26 260 40 300 48", SIGNAL),
        ("M0 118 C40 116 60 90 90 72 C140 60 200 56 300 54", BONE),
        ("M0 118 L40 118 C120 110 200 60 300 14", BLUE),
    ]
    paths = f'<path d="{grid}" stroke="#2A2A28" stroke-width="1" fill="none"/>' + "".join(
        f'<path d="{p(d)}" stroke="{c}" stroke-width="2.5" fill="none" stroke-linecap="square"/>' for d, c in traces
    )
    card = (
        f'<div class="dev" style="left:540px;top:110px;width:420px;height:280px">'
        f'<span style="position:absolute;left:16px;top:14px;font:400 13px \'IBM Plex Mono\';color:{MUTE}">P · F · W</span>'
        f'<span style="position:absolute;right:16px;top:14px;font:400 13px \'IBM Plex Mono\';color:{MUTE}">0:28</span>'
        f'<svg width="420" height="280" style="position:absolute;left:-1px;top:-1px">{paths}</svg>{ticks(418, 278, 12)}</div>'
    )
    logo = f'<img src="{(LOGO / "onlyshots-horizontal-on-dark.svg").as_uri()}" style="position:absolute;left:64px;top:154px;height:64px">'
    body = logo + copy(64, 250, 400, None, "FILM THE SHOT.\nKEEP THE DATA.", "Record espresso with its live graph from your machine and scale.", 30) + card
    return frame(1024, 500, 25, body)


def shoot(chrome: str, page: str, w: int, h: int, scale: float, out: pathlib.Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        src = pathlib.Path(tmp) / "page.html"
        src.write_text(page)
        subprocess.run(
            [chrome, "--headless=new", "--hide-scrollbars", "--allow-file-access-from-files", f"--window-size={w},{h}",
             f"--force-device-scale-factor={scale}", "--virtual-time-budget=4000", f"--screenshot={out}", src.as_uri()],
            check=True, capture_output=True,
        )
    drop_alpha(out)
    want = f"{round(w * scale)}x{round(h * scale)}"
    got = subprocess.run(
        ["magick", "identify", "-format", "%wx%h", out], check=True, capture_output=True, text=True
    ).stdout
    if got != want:
        raise SystemExit(f"{out.name}: wrote {got}, wanted {want}")


def drop_alpha(path: pathlib.Path) -> None:
    """Flattens onto ink and drops alpha: Play refuses alpha in the feature graphic and prefers none elsewhere."""
    subprocess.run(["magick", path, "-background", INK, "-alpha", "remove", "-alpha", "off", f"PNG24:{path}"], check=True)


def main() -> None:
    chrome = next((c for c in CHROMES if shutil.which(c) or pathlib.Path(c).exists()), None)
    if not chrome:
        raise SystemExit("needs Google Chrome or Chromium")
    for d in ("phone", "tablet-7", "tablet-10"):
        (PLAY / d).mkdir(parents=True, exist_ok=True)
    jobs = []
    for i, (scr, *c) in enumerate(PHONE, 1):
        body = copy(24, 32, 300, *c, 25) + device(54, 212, 236, 511, 8, 10, SCREENS / f"{scr}.png")
        jobs.append((frame(360, 640, 24, body), 360, 640, 3, PLAY / "phone" / f"{i:02}.png"))
    for i, (scr, *c) in enumerate(TABLET, 1):
        body = copy(40, 48, 520, *c, 36) + device(64, 300, 460, 736, 8, 12, SCREENS / f"{scr}-M.png")
        jobs.append((frame(600, 960, 32, body), 600, 960, 2, PLAY / "tablet-7" / f"{i:02}.png"))
        body = copy(64, 96, 360, *c, 44) + device(472, 152, 736, 460, 10, 14, SCREENS / f"{scr}-B.png")
        jobs.append((frame(1280, 800, 32, body), 1280, 800, 2, PLAY / "tablet-10" / f"{i:02}.png"))
    jobs.append((feature(), 1024, 500, 1, PLAY / "feature-graphic-1024x500.png"))
    for into, frames, find, fw, fh, cell, scale, (cx, cy, cw, hs), (dx, dy, dw) in FRAMED:
        tab, first = into
        dev = DEVICE_OF[into]
        shots = [(c, find(scr)) for scr, *c in frames]
        missing = [s.name for _, s in shots if not s.exists()]
        if missing:
            print(f"{tab} {first:02}+: skipped, no {', '.join(missing)}")
            continue
        (APPSTORE_DIR / tab).mkdir(parents=True, exist_ok=True)
        for i, (c, shot) in enumerate(shots, first):
            body = copy(cx, cy, cw, *c, hs) + framed(dx, dy, dw, shot, dev)
            jobs.append((frame(fw, fh, cell, body), fw, fh, scale, APPSTORE_DIR / tab / f"{i:02}.png"))
    for page, w, h, scale, out in jobs:
        shoot(chrome, page, w, h, scale, out)
        print(out.relative_to(HERE))
    # Play wants the icon as a 32-bit PNG.
    subprocess.run(["magick", LOGO / "android" / "play-store-512.png", f"PNG32:{PLAY / 'icon-512.png'}"], check=True)
    print("play/icon-512.png")


if __name__ == "__main__":
    main()
