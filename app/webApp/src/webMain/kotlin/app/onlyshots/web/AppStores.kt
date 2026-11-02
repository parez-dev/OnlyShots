package app.onlyshots.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.onlyshots.open.StoreLinks
import kotlinx.browser.localStorage
import kotlinx.browser.window

/** The phones' app in the stores. */
object AppStores {
    const val PLAY_PACKAGE = "dev.parez.onlyshots"

    /**
     * The App Store's numeric id (apps.apple.com/app/id…), the app's Apple ID in App Store Connect. Null hides every
     * App Store button. Also set in `resources/open/index.html`, the `apple-itunes-app` meta tag in `index.html` and
     * the link on `resources/unsupported/index.html`.
     */
    val APP_STORE_ID: String? = "6829853464"

    const val PLAY_URL = "https://play.google.com/store/apps/details?id=$PLAY_PACKAGE"

    /**
     * Chrome on Android opens the app when it is installed (it handles `onlyshots.vercel.app/open`), and the Play page
     * when it is not.
     */
    const val ANDROID_LINK =
        "intent://onlyshots.vercel.app/open#Intent;scheme=https;package=$PLAY_PACKAGE;" +
            "S.browser_fallback_url=https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3D$PLAY_PACKAGE;end"

    /** Safari never opens the app for a link on the same site: the App Store page says "Open" when it is installed. */
    val IOS_LINK: String?
        get() = APP_STORE_ID?.let { "https://apps.apple.com/app/id$it" }
}

private enum class Platform {
    Android,
    Ios,
    Other,
}

/** Which store this browser's device has, if any. iPadOS Safari calls itself a Mac, but a Mac has no touch screen. */
private fun platform(): Platform {
    val ua = window.navigator.userAgent
    return when {
        ua.contains("Android") -> Platform.Android
        ua.contains("iPhone") || ua.contains("iPad") -> Platform.Ios
        ua.contains("Macintosh") && window.navigator.maxTouchPoints > 1 -> Platform.Ios
        else -> Platform.Other
    }
}

private const val DISMISSED_KEY = "onlyshots.appBanner.dismissed"

/**
 * The store links for this browser, as the start screen shows them: an Android device gets Google Play (its link opens
 * the app when installed), an iPhone or iPad the App Store, a computer both, each only where the store has the app. On
 * a phone the banner can be closed, and stays closed in this browser.
 */
@Composable
fun rememberStoreLinks(): StoreLinks {
    var dismissed by remember {
        mutableStateOf(runCatching { localStorage.getItem(DISMISSED_KEY) == "1" }.getOrDefault(false))
    }
    val platform = remember { platform() }
    val go = { url: String -> { window.location.href = url } }
    return StoreLinks(
        onPlay =
            when (platform) {
                Platform.Android -> go(AppStores.ANDROID_LINK)
                Platform.Other -> go(AppStores.PLAY_URL)
                Platform.Ios -> null
            },
        onAppStore = AppStores.IOS_LINK?.takeIf { platform != Platform.Android }?.let(go),
        phone = isMobileBrowser(),
        bannerDismissed = dismissed,
        onDismissBanner = {
            dismissed = true
            runCatching { localStorage.setItem(DISMISSED_KEY, "1") }
        },
    )
}
