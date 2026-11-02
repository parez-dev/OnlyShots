#!/usr/bin/env python3
"""Builds the App Store creative assets from a shot exported by OnlyShots.

These are marketing art, not app previews: Apple sets them no device-capture rule, they loop forever, and they are
muted. So each is the app's own export — a real shot with its real graph over it — framed in the Instrument look.

    ./gradlew :app:desktopApp:run                                    # export the recording at 16:9 / 4K
    python3 design/header/build.py master.mp4 --in 12 --out 34       # the product page header
    python3 design/header/build.py master.mp4 --slot search --out 19 # the search results banner

Needs ffmpeg and Google Chrome (or the build Playwright keeps for tools/web-smoke, which render.py finds).

Spec, read 2026-10-29: developer.apple.com/help/app-store-connect/reference/app-information/creative-assets-specifications/

- header: 21:9 at 3840 x 1646. Muted by default, but a viewer can unmute, so its sound is worth keeping.
- search: 3:2 between 1920 x 1280 and 3840 x 2560. 3240 x 2160 cuts the master's width at native resolution, where
  the 16:9 option (5244 x 2950) would mean upscaling it by 1.37. Audio here can never be unmuted, so it is dropped.

Both are 30 or 60 fps and 5 to 30 s.
"""

import argparse
import pathlib
import shutil
import subprocess
import sys
import tempfile

HERE = pathlib.Path(__file__).parent
sys.path.insert(0, str(HERE.parent / "store"))
from render import CHROMES  # noqa: E402  the same Chrome discovery the store art uses

FPS = 30
MIN_S, MAX_S = 5, 30
# (width, height, furniture page, keep the sound, which axis of the master is cut). The header crops the master's height and the search banner its
# width, so each has its own furniture: the 21:9 copy block would not fit a slot people see at thumbnail size.
SLOTS = {
    "header": (3840, 1646, "furniture.html", True, "height"),
    "search": (3240, 2160, "furniture-search.html", False, "width"),
}
# Apple sets the header no bitrate, only 500 MB. 45 Mbps is what the app's own 4K export writes, so the
# compose pass stops throwing three quarters of it away; 30 s of it is about 170 MB. The 10-12 Mbps in the
# app preview spec is for an 886-wide clip and has no business here.
MAX_BYTES = 500_000_000


def chrome() -> str:
    found = next((c for c in CHROMES if shutil.which(c) or pathlib.Path(c).exists()), None)
    if not found:
        raise SystemExit("needs Google Chrome or Chromium")
    return found


def furniture(out: pathlib.Path, w: int, h: int, page: str) -> None:
    """Shoots the furniture page keeping its alpha, so ffmpeg can lay it over the footage."""
    subprocess.run(
        [chrome(), "--headless=new", "--hide-scrollbars", "--allow-file-access-from-files",
         "--default-background-color=00000000", f"--window-size={w},{h}", "--force-device-scale-factor=1",
         "--virtual-time-budget=4000", f"--screenshot={out}", (HERE / page).as_uri()],
        check=True, capture_output=True,
    )
    channels = subprocess.run(
        ["magick", "identify", "-format", "%[channels]", out], check=True, capture_output=True, text=True
    ).stdout
    # Every other page under design/ flattens its alpha away; this one must keep it or the overlay hides the shot.
    if "a" not in channels:
        raise SystemExit(f"furniture.png came out {channels}, with no alpha: the overlay would be opaque")


def compose(
    master: pathlib.Path,
    layer: pathlib.Path,
    start: float,
    end: float,
    out: pathlib.Path,
    top: int | None,
    left: int | None,
    slot: str,
) -> None:
    w, h, _, sound, cut = SLOTS[slot]
    length = end - start
    if not MIN_S <= length <= MAX_S:
        raise SystemExit(f"{length:g}s is outside Apple's {MIN_S}-{MAX_S}s")
    if cut == "height":
        # A 16:9 export is 2160 tall, so the header takes a band of it: centred that is 257 off each end, which cuts
        # the portafilter at the top of the frame; --top 0 keeps the top and drops the base.
        y = "(ih-oh)/2" if top is None else str(top)
        crop = f"scale={w}:-2,crop={w}:{h}:0:{y}"
    else:
        # Taller than the master, so it is the width that goes: scale to the height and cut the width. Centred is
        # the default, but a graph placed to one side needs that side kept, hence --left.
        x = "(iw-ow)/2" if left is None else str(left)
        crop = f"scale=-2:{h},crop={w}:{h}:{x}:0"
    chain = f"[0:v]{crop},fps={FPS},setsar=1[v];[v][1:v]overlay=0:0:format=auto[o]"
    audio = ["-map", "0:a?", "-c:a", "aac", "-b:a", "256k", "-ac", "2", "-ar", "48000"] if sound else ["-an"]
    subprocess.run(
        ["ffmpeg", "-y", "-ss", f"{start}", "-to", f"{end}", "-i", master, "-i", layer,
         "-filter_complex", chain, "-map", "[o]", *audio,
         "-c:v", "libx264", "-profile:v", "high", "-preset", "slow",
         "-b:v", "45M", "-maxrate", "55M", "-bufsize", "90M", "-pix_fmt", "yuv420p", "-movflags", "+faststart",
         out],
        check=True, capture_output=True,
    )
    probe = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=width,height",
         "-of", "csv=p=0:s=x", out], check=True, capture_output=True, text=True
    ).stdout.strip()
    # A wrong size is otherwise caught only by App Store Connect refusing the upload.
    if probe != f"{w}x{h}":
        raise SystemExit(f"{out.name}: wrote {probe}, wanted {w}x{h}")
    if out.stat().st_size > MAX_BYTES:
        raise SystemExit(f"{out.name}: {out.stat().st_size / 1e6:.0f} MB, over Apple's 500 MB")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("master", type=pathlib.Path, help="the 16:9 export out of OnlyShots")
    ap.add_argument("--in", dest="start", type=float, default=0.0, help="seconds into the master to start")
    ap.add_argument("--out", dest="end", type=float, required=True, help="seconds into the master to stop")
    ap.add_argument("--slot", choices=sorted(SLOTS), default="header", help="which creative asset to build")
    ap.add_argument("--to", type=pathlib.Path, default=None)
    ap.add_argument("--top", type=int, default=None, help="top of the band in the 2160 frame (header; default centred)")
    ap.add_argument("--left", type=int, default=None, help="left of the cut in the 3840 frame (search; default centred)")
    a = ap.parse_args()
    if not a.master.exists():
        raise SystemExit(f"no such master: {a.master}")
    w, h, page, _, _ = SLOTS[a.slot]
    a.to = a.to or HERE / "out" / f"{a.slot}-{w}x{h}.mp4"
    a.to.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        layer = pathlib.Path(tmp) / "furniture.png"
        furniture(layer, w, h, page)
        compose(a.master, layer, a.start, a.end, a.to, a.top, a.left, a.slot)
    size = a.to.stat().st_size / 1e6
    root = HERE.parent.parent
    where = a.to.relative_to(root) if a.to.is_relative_to(root) else a.to
    print(f"{where}  {w}x{h}  {a.end - a.start:g}s  {size:.1f} MB")


if __name__ == "__main__":
    main()
