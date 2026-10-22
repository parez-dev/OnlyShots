import KotlinRuntime
import OnlyShotsApp
import UIKit

/**
 * The app's native shell on iOS 27.1: a navigation controller around the one Compose screen, so the system shows each
 * screen's back key and actions in its own bar. On iPhone Duo that bar is the side column (with the status and the
 * Dynamic Island) on the outer display and on the inner one held sideways, a top bar only on the inner display upright.
 * Without a hinge the bar stays hidden and the screens draw their own, as on any other iPhone.
 */
@available(iOS 27.1, *)
final class ShellNavigationController: UINavigationController {
    init() {
        super.init(rootViewController: ComposeHostController())
        // Glass items over the app's own ink: no bar background, bone symbols, the prominent item signal orange.
        let bar = UINavigationBarAppearance()
        bar.configureWithTransparentBackground()
        navigationBar.standardAppearance = bar
        navigationBar.scrollEdgeAppearance = bar
        navigationBar.compactAppearance = bar
        let tools = UIToolbarAppearance()
        tools.configureWithTransparentBackground()
        toolbar.standardAppearance = tools
        toolbar.scrollEdgeAppearance = tools
        view.tintColor = ShellColors.bone
        setNavigationBarHidden(true, animated: false)
        setToolbarHidden(true, animated: false)
    }

    required init?(coder: NSCoder) { fatalError("not from a storyboard") }

    override var childForStatusBarStyle: UIViewController? { topViewController }
    override var childForHomeIndicatorAutoHidden: UIViewController? { topViewController }
}

/**
 * Holds the Compose screen, full bounds under the bars (which reach it as safe-area insets). Tells Kotlin about the
 * fold, from a `UIHingeInteraction` and the view's active division region, and builds the navigation item from the bar
 * Kotlin hands over ([NativeShell]).
 */
@available(iOS 27.1, *)
final class ComposeHostController: UIViewController {
    private let content = NativeMainViewController()
    private let listener = BarListener()
    private var hasHinge = false
    private var partiallyOpen = false
    private var bottomItems: [UIBarButtonItem] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        edgesForExtendedLayout = .all
        extendedLayoutIncludesOpaqueBars = true
        navigationItem.largeTitleDisplayMode = .never
        addChild(content)
        content.view.frame = view.bounds
        content.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(content.view)
        content.didMove(toParent: self)

        listener.onBar = { [weak self] bar in self?.apply(bar) }
        NativeShell.shared.listener = listener
        view.addInteraction(UIHingeInteraction { [weak self] _, update in
            guard let self else { return }
            self.hasHinge = update.hinge != nil
            self.partiallyOpen = update.hinge?.status == .partiallyOpen
            self.showBars()
            self.reportFold()
        })
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        reportFold()
    }

    override var childForStatusBarStyle: UIViewController? { content }
    override var childForHomeIndicatorAutoHidden: UIViewController? { content }

    /** The system bars where there is a hinge (a foldable iPhone); the screens' own everywhere else. */
    private func showBars() {
        NativeShell.shared.setEnabled(on: hasHinge)
        navigationController?.setNavigationBarHidden(!hasHinge, animated: false)
        navigationController?.setToolbarHidden(!hasHinge || bottomItems.isEmpty, animated: false)
    }

    /**
     * Where the fold crosses the window while it divides it, in points: the active division region, inset by the
     * margins it carries for touch targets around it.
     */
    private func reportFold() {
        guard hasHinge, let region = view.reservedRegions(kind: .division).first(where: { $0.isActive }) else {
            foldGone()
            return
        }
        let fold = region.frame.inset(by: region.margins)
        foldChanged(
            partiallyOpen: partiallyOpen,
            left: fold.minX, top: fold.minY, right: fold.maxX, bottom: fold.maxY
        )
    }

    /** The screen in front's bar: back first, then its items in order, grouped; the bottom ones in the toolbar. */
    private func apply(_ bar: BarSpec) {
        navigationItem.title = nil
        navigationItem.leftBarButtonItem = bar.back ? backItem(enabled: bar.backEnabled) : nil

        // Each item alone, except those sharing a group number, which sit together where the first of them comes.
        var slots: [[UIBarButtonItem]] = []
        var slotOfGroup: [Int: Int] = [:]
        for item in bar.items where !item.bottom {
            if item.group != 0, let slot = slotOfGroup[item.group] {
                slots[slot].append(barItem(item))
                continue
            }
            if item.group != 0 { slotOfGroup[item.group] = slots.count }
            slots.append([barItem(item)])
        }
        navigationItem.trailingItemGroups = slots.map {
            UIBarButtonItemGroup(barButtonItems: $0, representativeItem: nil)
        }

        bottomItems = bar.items.filter(\.bottom).map(barItem)
        setToolbarItems(bottomItems, animated: false)
        showBars()
    }

    private func backItem(enabled: Bool) -> UIBarButtonItem {
        let item = UIBarButtonItem(
            title: NSLocalizedString("Back", comment: "The native bar's back key"),
            image: UIImage(systemName: "chevron.backward"),
            primaryAction: UIAction { _ in NativeShell.shared.back() },
            menu: nil
        )
        item.isEnabled = enabled
        item.accessibilityIdentifier = "back"
        return item
    }

    private func barItem(_ spec: BarSpec.Item) -> UIBarButtonItem {
        let id = spec.id
        let item = UIBarButtonItem(
            title: spec.title,
            image: UIImage(systemName: ShellSymbols.name(for: spec.icon)),
            primaryAction: UIAction { _ in NativeShell.shared.tapped(id: id) },
            menu: nil
        )
        item.isEnabled = spec.enabled
        item.accessibilityIdentifier = id
        item.accessibilityLabel = spec.title
        if spec.prominent {
            item.style = .prominent
            item.tintColor = ShellColors.signal
        }
        switch spec.priority {
        case "High": item.visibilityPriority = .high
        case "Low": item.visibilityPriority = .low
        default: item.visibilityPriority = .standard
        }
        return item
    }
}

/** Kotlin's bar, as it crosses over (`NativeShell.encode`). */
struct BarSpec: Decodable {
    struct Item: Decodable {
        let id: String
        let title: String
        let icon: String
        let prominent: Bool
        let enabled: Bool
        let group: Int
        let bottom: Bool
        let priority: String
    }

    let back: Bool
    let backEnabled: Bool
    let items: [Item]
}

/** Hears the bar in front change in Kotlin; called on the main thread, from composition. */
final class BarListener: KotlinBase, NativeShellListener {
    var onBar: ((BarSpec) -> Void)?

    func barChanged(json: String) {
        guard let data = json.data(using: .utf8), let bar = try? JSONDecoder().decode(BarSpec.self, from: data)
        else { return }
        onBar?(bar)
    }
}

/** The SF Symbol for each of Kotlin's `BarIcon`s, so the bar's glass items look like the system's own. */
enum ShellSymbols {
    static func name(for icon: String) -> String {
        switch icon {
        case "Close": "xmark"
        case "Export": "square.and.arrow.up"
        case "Undo": "arrow.uturn.backward"
        case "Redo": "arrow.uturn.forward"
        case "SoundOn": "speaker.wave.2"
        case "SoundOff": "speaker.slash"
        case "Help": "questionmark.circle"
        case "Privacy": "hand.raised"
        case "Record": "record.circle"
        case "Settings": "slider.horizontal.3"
        case "Add": "plus"
        case "File": "doc"
        case "Open": "checkmark"
        case "Save": "checkmark"
        default: "circle"
        }
    }
}

/** The Instrument colours the bar's items take (Tokens.kt: Brand.Bone, Brand.Signal). */
enum ShellColors {
    static let bone = UIColor(red: 0xE8 / 255.0, green: 0xE6 / 255.0, blue: 0xE1 / 255.0, alpha: 1)
    static let signal = UIColor(red: 0xFF / 255.0, green: 0x5A / 255.0, blue: 0x1F / 255.0, alpha: 1)
}
