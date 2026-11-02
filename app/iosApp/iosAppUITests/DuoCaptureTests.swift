import UIKit
import XCTest

/// iPhone Duo store screenshots: the installed app as the system shows it, with the side column, the status and the
/// Dynamic Island. The side column is iOS's own `UINavigationBar` on a hinged device (`NativeShell.swift`), so it
/// exists in no Compose preview and in no layoutlib golden — a simulator is the only place it can be pictured.
///
/// `tools/ios-screenshots/capture.sh` folds the simulator (XCTest cannot move the hinge) and runs this once per pose:
///
/// - `DUO_POSE`: `closed`, `open` or `half-open`; without it the test is skipped, so the smoke tests run as before.
/// - `DUO_OUT`: where the pictures are written.
///
/// Capture only: nothing is compared and nothing fails on a visual change. This is art, not a gate — the screenshot
/// tests that did compare were deliberately removed in #20.
final class DuoCaptureTests: XCTestCase {
    private let app = XCUIApplication()
    private let env = ProcessInfo.processInfo.environment

    override func setUpWithError() throws {
        try XCTSkipIf(env["DUO_POSE"] == nil, "run by tools/ios-screenshots/capture.sh, which folds the simulator")
        continueAfterFailure = true
        // A fresh simulator asks about the local network when Home starts searching for machines.
        addUIInterruptionMonitor(withDescription: "System alert") { alert in
            for label in ["Allow", "OK", "Don’t Allow"] where alert.buttons[label].exists {
                alert.buttons[label].tap()
                return true
            }
            return false
        }
    }

    override func tearDown() {
        app.terminate()
    }

    func testScreens() throws {
        app.launch()
        waitFor("screen.start", timeout: 30)

        shoot("start")

        tap("start.help")
        tap("help.privacy")
        waitFor("screen.privacy")
        shoot("privacy")
        tap("back")
        waitFor("screen.start")

        tap("start.compose")
        waitFor("screen.open")
        shoot("open")
        tap("back")
        waitFor("screen.start")

        tap("start.record")
        waitFor("screen.home")
        shoot("home")
        tap("back")
        waitFor("screen.start")
    }

    // No editor screen here, and no record screen either.
    //
    // Record cannot be reached at all: the rail's record key is `enabled = !take.state.noCamera`
    // (TakeWidgets.kt), and a simulator has no camera.
    //
    // The editor can only be reached through "compose a video", which needs a clip picked from the system picker,
    // and XCUITest cannot drive that picker here. Everything the app does works — the action sheet opens, Photos
    // presents PHPicker with the clip in it, Files presents the document browser — but taps into those
    // out-of-process views are ignored: PHPicker's grid items (`PXGGridLayout-Info` images) swallow a tap and a
    // coordinate tap alike, and the document browser stays on Recents however often its Browse tab is tapped.
    // In-app controls and the alert's own buttons tap normally, so this is the picker, not the app.
    //
    // The Open screen below is captured instead, and shows the same flow with its shot already fetched.

    // MARK: - Pictures

    private func shoot(_ screenName: String) {
        settle()
        // Named by pose and by the way the app's window lies, which is the way up the screen is held.
        let window = app.windows.firstMatch.frame
        let wide = window.width > window.height
        let name = "\(env["DUO_POSE"] ?? "")-\(wide ? "landscape" : "portrait")-\(screenName)"
        var shot = screen().screenshot().image.cgImage!
        // The inner display's picture comes as its panel lies, not as the app is drawn: a new simulator leaves it a
        // quarter turn clockwise, which is three of these. One counter-clockwise turn, which is what this did while
        // it wrote goldens, lands the picture head to foot. Check the direction if a later Xcode moves the panel.
        if wide != (shot.width > shot.height) {
            shot = turnedCounterclockwise(turnedCounterclockwise(turnedCounterclockwise(shot)))
        }
        let attachment = XCTAttachment(image: UIImage(cgImage: shot))
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
        guard let out = env["DUO_OUT"] else { return XCTFail("DUO_OUT not set") }
        write(shot, to: URL(fileURLWithPath: out).appendingPathComponent("\(name).png"))
    }

    /**
     * The display the app is on. `XCUIScreen.main` stays the outer display even while the phone is open, so the open
     * poses take the largest screen: the inner display.
     */
    private func screen() -> XCUIScreen {
        if env["DUO_POSE"] == "closed" { return XCUIScreen.main }
        return XCUIScreen.screens.max { area($0) < area($1) } ?? XCUIScreen.main
    }

    private func area(_ screen: XCUIScreen) -> Int {
        let image = screen.screenshot().image
        return Int(image.size.width * image.scale * image.size.height * image.scale)
    }

    /** Waits for motion to end: the splash hand-off, screen transitions, the bar's own. */
    private func settle() {
        _ = XCTWaiter.wait(for: [XCTestExpectation(description: "settle")], timeout: 1.5)
    }

    private func turnedCounterclockwise(_ image: CGImage) -> CGImage {
        let context = rgbaContext(image.height, image.width)
        context.translateBy(x: CGFloat(image.height), y: 0)
        context.rotate(by: .pi / 2)
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        return context.makeImage()!
    }

    private func rgbaContext(_ width: Int, _ height: Int) -> CGContext {
        CGContext(
            data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4,
            space: CGColorSpace(name: CGColorSpace.sRGB)!,
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        )!
    }

    private func write(_ image: CGImage, to url: URL) {
        try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        guard let data = UIImage(cgImage: image).pngData() else { return }
        do {
            try data.write(to: url)
        } catch {
            XCTFail("could not write \(url.path): \(error)")
        }
    }

    // MARK: - Finding controls by their test tags (app.onlyshots.TestTags), as AppSmokeTests does

    private func element(_ tag: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: tag).firstMatch
    }

    private func waitFor(_ tag: String, timeout: TimeInterval = 10) {
        XCTAssertTrue(element(tag).waitForExistence(timeout: timeout), "\(tag) not on screen after \(timeout) s")
    }

    private func tap(_ tag: String, timeout: TimeInterval = 10) {
        waitFor(tag, timeout: timeout)
        element(tag).tap()
    }
}
