import OnlyShotsApp
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // iOS 27.1 brings the hinge and the vertical bars: the native shell (NativeShell.swift) watches the fold and
        // shows the screens' bars natively where there is one. Earlier systems have no foldable iPhone to adapt to.
        if #available(iOS 27.1, *) {
            return ShellNavigationController()
        }
        return MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
    }
}
