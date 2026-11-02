#!/usr/bin/env bash
# iPhone Duo store screenshots: the installed app on the iPhone Duo simulator, closed, open and half-open, as the
# system shows it — side column, status, Dynamic Island. That side column is iOS's own navigation bar on a hinged
# device (app/iosApp/iosApp/NativeShell.swift), so no Compose preview and no layoutlib golden can contain it; a
# simulator is the only place it exists.
#
#   tools/ios-screenshots/capture.sh        writes app/iosApp/build/duo-screenshots/<pose>-<way up>-<screen>.png
#
# Capture only. The screenshot tests that compared against goldens were removed in #20 and stay removed; nothing here
# runs in CI or fails on a visual change.
#
# Needs Xcode 27.1+ (iPhone Duo simulator, iOS 27.1), xcodegen and Node (npm, for mobilecli). Runs on its own
# simulator, "OnlyShots Duo screenshots", made on first use; DUO_UDID picks another, MOBILECLI another mobilecli.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
ios="$root/app/iosApp"
out="$ios/build/duo-screenshots"
derived="$ios/build/DerivedData"
rm -rf "$out"
mkdir -p "$out"

# mobilecli 1.0.21: the first with `device fold` for iOS simulators. XCTest cannot move the hinge.
mobilecli="${MOBILECLI:-}"
if [[ -z "$mobilecli" ]]; then
    tools="$root/build/tools/mobilecli"
    mobilecli="$tools/node_modules/@mobilenext/mobilecli-darwin-arm64/mobilecli-darwin-arm64"
    [[ -x "$mobilecli" ]] || npm install --silent --no-save --prefix "$tools" @mobilenext/mobilecli-darwin-arm64@1.0.21
fi

name="OnlyShots Duo screenshots"
udid="${DUO_UDID:-$(xcrun simctl list devices available -j | python3 -c '
import json, sys
for runtime, devices in json.load(sys.stdin)["devices"].items():
    for d in devices:
        if d["name"] == sys.argv[1]:
            print(d["udid"]); sys.exit()
' "$name")}"
if [[ -z "$udid" ]]; then
    # By what supports an iPhone Duo, not by version: as of Xcode 27.2 only the 27.1 runtime does, and asking 27.2
    # for one fails with "Incompatible device".
    runtime="$(xcrun simctl list runtimes -j | python3 -c '
import json, sys
ok = [
    r["identifier"]
    for r in json.load(sys.stdin)["runtimes"]
    if r["identifier"].startswith("com.apple.CoreSimulator.SimRuntime.iOS-")
    and any("Duo" in t["name"] for t in r.get("supportedDeviceTypes", []))
]
if not ok:
    sys.exit("no installed iOS runtime offers an iPhone Duo; install the iOS 27.1 simulator runtime in Xcode")
print(ok[-1])
')"
    udid="$(xcrun simctl create "$name" com.apple.CoreSimulator.SimDeviceType.iPhone-Duo "$runtime")"
fi
echo "iPhone Duo simulator $udid"

xcrun simctl boot "$udid" 2>/dev/null || true
xcrun simctl bootstatus "$udid" -b >/dev/null
xcrun simctl ui "$udid" appearance dark
# The status bar Apple puts on its own marketing shots.
xcrun simctl status_bar "$udid" override --time 9:41 --dataNetwork wifi --wifiMode active --wifiBars 3 \
    --cellularMode active --cellularBars 4 --batteryState charged --batteryLevel 100
# mobilecli folds through its own agent on the simulator; a fresh simulator needs it installed once, and its daemon
# can take a moment to see a simulator that has just booted.
for _ in $(seq 30); do
    "$mobilecli" devices --platform ios --type simulator 2>/dev/null | grep -q "$udid" && break
    sleep 2
done
if ! "$mobilecli" agent install --device "$udid" >/dev/null; then
    echo "mobilecli does not see $udid; it sees:" >&2
    "$mobilecli" devices --include-offline --platform ios --type simulator -v >&2 || true
    exit 1
fi
xcrun simctl privacy "$udid" grant camera dev.parez.onlyshots 2>/dev/null || true
xcrun simctl privacy "$udid" grant microphone dev.parez.onlyshots 2>/dev/null || true

(cd "$ios" && xcodegen generate --quiet)
xcodebuild build-for-testing -project "$ios/iosApp.xcodeproj" -scheme iosApp -destination "id=$udid" \
    -derivedDataPath "$derived" -quiet

# Folding goes through mobilecli's agent, a UI test runner on the simulator: the first start can outlast its wait.
fold() {
    for attempt in 1 2 3; do
        "$mobilecli" device fold "$1" --device "$udid" >/dev/null && return 0
        echo "fold $1: attempt $attempt failed, retrying" >&2
        sleep 10
    done
    return 1
}

failed=0
# DUO_POSES narrows the run while working on one of them.
for pose in ${DUO_POSES:-closed open half-open}; do
    echo "· $pose"
    fold "$([[ "$pose" == closed ]] && echo folded || echo "$pose")"
    sleep 2
    rm -rf "$out/$pose.xcresult"
    TEST_RUNNER_DUO_POSE="$pose" TEST_RUNNER_DUO_OUT="$out" \
        xcodebuild test-without-building -project "$ios/iosApp.xcodeproj" -scheme iosApp -destination "id=$udid" \
        -derivedDataPath "$derived" -only-testing:iosAppUITests/DuoCaptureTests \
        -resultBundlePath "$out/$pose.xcresult" -quiet || failed=1
done

fold open || true
xcrun simctl status_bar "$udid" clear

echo "pictures in ${out#"$root/"}"
ls "$out"/*.png 2>/dev/null | sed "s|$out/|  |" || echo "  none written" >&2
exit "$failed"
