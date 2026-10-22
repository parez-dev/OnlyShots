import UIKit
import XCTest

/// iPhone Duo goldens: the installed app's screens as the system shows them, its bars, status and Dynamic Island
/// included, in each pose. `tools/ios-screenshots/run.sh` folds the simulator (XCTest cannot move the hinge)
/// and runs this once per pose, passing:
///
/// - `DUO_POSE`: `closed`, `open` or `half-open`; without it the test is skipped, so the smoke tests run as before.
/// - `DUO_GOLDENS`: the folder of checked-in goldens (`app/iosApp/screenshots/duo`).
/// - `DUO_OUT`: where a failed comparison leaves the new picture and the difference.
/// - `DUO_RECORD=1`: write the goldens instead of comparing.
///
/// Pictures are kept at half size; two match when at most 0.2 % of their pixels differ by more than a little, which
/// lets anti-aliasing and the odd blinking caret through but not a moved control.
final class DuoScreenshotTests: XCTestCase {
    private let app = XCUIApplication()
    private let env = ProcessInfo.processInfo.environment
    /** How the inner display's picture is turned upright; found on the first screen, the same for the whole run. */
    private var turn: ((CGImage) -> CGImage)?

    override func setUpWithError() throws {
        try XCTSkipIf(env["DUO_POSE"] == nil, "run by tools/ios-screenshots/run.sh, which folds the simulator per pose")
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

    /// One pass in the pose and way up the simulator is in: XCTest turns neither the hinge nor, on iPhone Duo, the inner
    /// display, so the runner folds it and the other way up is a second run after turning the simulator by hand.
    func testScreens() throws {
        app.launch()
        waitFor("screen.start", timeout: 30)

        check("start")

        tap("start.help")
        tap("help.privacy")
        waitFor("screen.privacy")
        check("privacy")
        tap("back")
        waitFor("screen.start")

        tap("start.compose")
        waitFor("screen.open")
        check("open")
        tap("back")
        waitFor("screen.start")

        tap("start.record")
        waitFor("screen.home")
        check("home")
        tap("back")
        waitFor("screen.start")
    }

    // MARK: - Golden pictures

    private func check(_ screenName: String) {
        settle()
        // Named by pose and by the way the app's window lies, which is the way up the screen is held.
        let window = app.windows.firstMatch.frame
        let wide = window.width > window.height
        let name = "\(env["DUO_POSE"] ?? "")-\(wide ? "landscape" : "portrait")-\(screenName)"
        var full = screen().screenshot().image.cgImage!
        // The inner display's picture comes as its panel lies; turn it the way the app is drawn, before halving it, so
        // either way the simulator is turned gives the same pixels (the panel is an odd number of pixels across).
        if wide != (full.width > full.height) { full = upright(full, window: window) }
        let shot = halfSize(full)
        let attachment = XCTAttachment(image: UIImage(cgImage: shot))
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)

        guard let goldens = env["DUO_GOLDENS"] else {
            XCTFail("DUO_GOLDENS not set")
            return
        }
        let golden = URL(fileURLWithPath: goldens).appendingPathComponent("\(name).png")
        if env["DUO_RECORD"] == "1" {
            write(shot, to: golden)
            return
        }
        guard let data = try? Data(contentsOf: golden), let expected = UIImage(data: data)?.cgImage else {
            XCTFail("no golden \(golden.lastPathComponent): record with tools/ios-screenshots/run.sh record")
            return
        }
        let (fraction, diff) = compare(shot, expected)
        if fraction > 0.002 {
            let out = URL(fileURLWithPath: env["DUO_OUT"] ?? NSTemporaryDirectory())
            write(shot, to: out.appendingPathComponent("\(name).png"))
            if let diff { write(diff, to: out.appendingPathComponent("\(name)-diff.png")) }
            XCTFail(String(format: "%@ differs from its golden in %.2f %% of pixels", name, fraction * 100))
        }
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

    private func halfSize(_ source: CGImage) -> CGImage {
        let width = source.width / 2
        let height = source.height / 2
        let context = rgbaContext(width, height)
        context.interpolationQuality = .high
        context.draw(source, in: CGRect(x: 0, y: 0, width: width, height: height))
        return context.makeImage()!
    }

    /**
     * Turns a sideways picture upright. Which way depends on which way the simulator is turned, so the first time it
     * tries both and keeps the one that puts the start screen's orange "record a shot" card where its frame says.
     */
    private func upright(_ shot: CGImage, window: CGRect) -> CGImage {
        if let turn { return turn(shot) }
        let card = element("start.record")
        let ways: [(CGImage) -> CGImage] = [turnedCounterclockwise, turnedClockwise]
        if card.exists {
            // Near the card's top right corner: its orange ground, clear of its words and icon.
            let probe = CGPoint(x: card.frame.maxX - 8 - window.minX, y: card.frame.minY + 8 - window.minY)
            for way in ways {
                let turned = way(shot)
                let scale = Double(turned.width) / window.width
                if isSignal(turned, x: Int(probe.x * scale), y: Int(probe.y * scale)) {
                    turn = way
                    return turned
                }
            }
        }
        XCTFail("could not tell which way the inner display is turned")
        turn = turnedCounterclockwise
        return turnedCounterclockwise(shot)
    }

    /** Whether the pixel is the signal orange (#FF5A1F), give or take. */
    private func isSignal(_ image: CGImage, x: Int, y: Int) -> Bool {
        guard x >= 0, y >= 0, x < image.width, y < image.height else { return false }
        let p = pixels(image)
        let i = (y * image.width + x) * 4
        return p[i] > 200 && (50...130).contains(p[i + 1]) && p[i + 2] < 90
    }

    private func turnedClockwise(_ image: CGImage) -> CGImage {
        let context = rgbaContext(image.height, image.width)
        context.translateBy(x: 0, y: CGFloat(image.width))
        context.rotate(by: -.pi / 2)
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        return context.makeImage()!
    }

    private func turnedCounterclockwise(_ image: CGImage) -> CGImage {
        let context = rgbaContext(image.height, image.width)
        context.translateBy(x: CGFloat(image.height), y: 0)
        context.rotate(by: .pi / 2)
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        return context.makeImage()!
    }

    /** The share of pixels that differ visibly, and a picture of them (red on a dimmed golden). */
    private func compare(_ actual: CGImage, _ expected: CGImage) -> (Double, CGImage?) {
        guard actual.width == expected.width, actual.height == expected.height else { return (1, nil) }
        let width = actual.width
        let height = actual.height
        let a = pixels(actual)
        let e = pixels(expected)
        var diff = [UInt8](repeating: 0, count: width * height * 4)
        var differing = 0
        for i in stride(from: 0, to: a.count, by: 4) {
            let delta = max(absDiff(a[i], e[i]), absDiff(a[i + 1], e[i + 1]), absDiff(a[i + 2], e[i + 2]))
            if delta > 24 {
                differing += 1
                diff[i] = 255
                diff[i + 3] = 255
            } else {
                diff[i] = e[i] / 4
                diff[i + 1] = e[i + 1] / 4
                diff[i + 2] = e[i + 2] / 4
                diff[i + 3] = 255
            }
        }
        let context = rgbaContext(width, height)
        context.data!.copyMemory(from: diff, byteCount: diff.count)
        return (Double(differing) / Double(width * height), context.makeImage())
    }

    private func absDiff(_ x: UInt8, _ y: UInt8) -> Int { abs(Int(x) - Int(y)) }

    private func pixels(_ image: CGImage) -> [UInt8] {
        let context = rgbaContext(image.width, image.height)
        context.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        let count = image.width * image.height * 4
        return Array(UnsafeBufferPointer(start: context.data!.assumingMemoryBound(to: UInt8.self), count: count))
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
