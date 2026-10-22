#!/usr/bin/env bash
# iPhone Duo goldens (app/iosApp/screenshots/duo): the installed app on the iPhone Duo simulator, closed, open and
# half-open, as the system shows it (side column, status, Dynamic Island). XCTest cannot move the hinge, so this folds
# the simulator with mobilecli and runs DuoScreenshotTests once per pose. The inner display stays the way a new
# simulator starts: sideways (neither XCTest nor mobilecli turns it).
#
#   tools/ios-screenshots/run.sh            compare with the goldens (fails on a difference; see build/duo-screenshots)
#   tools/ios-screenshots/run.sh record     write the goldens after a wanted visual change
#
# Needs Xcode 27.1 (iPhone Duo simulator, iOS 27.1), xcodegen and Node (npm, for mobilecli). It runs on its own
# simulator, "OnlyShots Duo screenshots", made on first use; DUO_UDID picks another, MOBILECLI another mobilecli binary.
set -euo pipefail

mode="${1:-verify}"
case "$mode" in record | verify) ;; *) echo "usage: $0 [record|verify]" >&2; exit 2 ;; esac

root="$(cd "$(dirname "$0")/../.." && pwd)"
ios="$root/app/iosApp"
goldens="$ios/screenshots/duo"
out="$ios/build/duo-screenshots"
derived="$ios/build/DerivedData"
mkdir -p "$out"

# mobilecli 1.0.21: the first with `device fold` for iOS simulators.
mobilecli="${MOBILECLI:-}"
if [[ -z "$mobilecli" ]]; then
    tools="$root/build/tools/mobilecli"
    mobilecli="$tools/node_modules/@mobilenext/mobilecli-darwin-arm64/mobilecli-darwin-arm64"
    [[ -x "$mobilecli" ]] || npm install --silent --no-save --prefix "$tools" @mobilenext/mobilecli-darwin-arm64@1.0.21
fi

# A simulator of its own, never turned by hand: an iPhone Duo turned the other way lays the screen out a pixel apart, so
# the goldens hold only for the way a new one starts.
name="OnlyShots Duo screenshots"
udid="${DUO_UDID:-$(xcrun simctl list devices available -j | python3 -c '
import json, sys
for runtime, devices in json.load(sys.stdin)["devices"].items():
    for d in devices:
        if d["name"] == sys.argv[1]:
            print(d["udid"]); sys.exit()
' "$name")}"
if [[ -z "$udid" ]]; then
    runtime="$(xcrun simctl list runtimes -j | python3 -c '
import json, sys
print([r["identifier"] for r in json.load(sys.stdin)["runtimes"] if r["identifier"].startswith("com.apple.CoreSimulator.SimRuntime.iOS-27")][-1])
')"
    udid="$(xcrun simctl create "$name" com.apple.CoreSimulator.SimDeviceType.iPhone-Duo "$runtime")"
fi
echo "iPhone Duo simulator $udid"

xcrun simctl boot "$udid" 2>/dev/null || true
xcrun simctl bootstatus "$udid" -b >/dev/null
xcrun simctl ui "$udid" appearance dark
# The same status everywhere, so it never makes a difference.
xcrun simctl status_bar "$udid" override --time 9:41 --dataNetwork wifi --wifiMode active --wifiBars 3 \
    --cellularMode active --cellularBars 4 --batteryState charged --batteryLevel 100
# mobilecli folds through its own agent on the simulator; a fresh simulator needs it installed once. Its daemon can take
# a moment to see a simulator that has just booted.
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

# Folding goes through mobilecli's agent, a UI test runner on the simulator: the first start can outlast its wait, so
# try a few times.
fold() {
    for attempt in 1 2 3; do
        "$mobilecli" device fold "$1" --device "$udid" >/dev/null && return 0
        echo "fold $1: attempt $attempt failed, retrying" >&2
        sleep 10
    done
    return 1
}

failed=0
for pose in closed open half-open; do
    echo "· $pose"
    fold "$([[ "$pose" == closed ]] && echo folded || echo "$pose")"
    sleep 2
    rm -rf "$out/$pose.xcresult"
    TEST_RUNNER_DUO_POSE="$pose" \
        TEST_RUNNER_DUO_GOLDENS="$goldens" \
        TEST_RUNNER_DUO_OUT="$out" \
        TEST_RUNNER_DUO_RECORD="$([[ "$mode" == record ]] && echo 1 || echo 0)" \
        xcodebuild test-without-building -project "$ios/iosApp.xcodeproj" -scheme iosApp -destination "id=$udid" \
        -derivedDataPath "$derived" -only-testing:iosAppUITests/DuoScreenshotTests \
        -resultBundlePath "$out/$pose.xcresult" -quiet || failed=1
done

fold open || true
xcrun simctl status_bar "$udid" clear

if [[ "$mode" == record ]]; then
    echo "goldens written to ${goldens#"$root/"}"
elif [[ "$failed" == 1 ]]; then
    echo "differences: ${out#"$root/"} (new pictures and *-diff.png; the .xcresult bundles hold every screen)" >&2
fi
exit "$failed"
