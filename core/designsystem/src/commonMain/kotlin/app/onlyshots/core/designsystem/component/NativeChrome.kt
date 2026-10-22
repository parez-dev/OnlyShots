package app.onlyshots.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/** The symbol of a bar item; each host maps it to its own glyph (SF Symbols on iOS). */
enum class BarIcon {
    Close,
    Export,
    Undo,
    Redo,
    SoundOn,
    SoundOff,
    Help,
    Privacy,
    Record,
    Settings,
    Add,
    File,
    Open,
    Save,
}

/** Which items the system bar drops into its overflow first when it runs out of room. */
enum class BarPriority {
    Low,
    Standard,
    High,
}

/**
 * One item of a screen's system bar. Items with the same [group] (above 0) sit together; [bottom] ones go to the far
 * end of the bar (the lower region of a vertical one).
 */
data class NativeBarItem(
    val id: String,
    val title: String,
    val icon: BarIcon,
    val prominent: Boolean = false,
    val enabled: Boolean = true,
    val group: Int = 0,
    val bottom: Boolean = false,
    val priority: BarPriority = BarPriority.Standard,
)

/** What the system bar shows for the screen in front: a back key and the items, in order. */
data class NativeBar(val back: Boolean, val backEnabled: Boolean, val items: List<NativeBarItem>)

/**
 * The screens' bar contributions, for a host that shows them in a system bar (iPhone Duo's side column) instead of the
 * screens drawing their own. Each screen (or overlay) registers under its own owner; the last one registered is the one
 * in front, so a pushed screen covers the one under it and popping it brings that one back.
 */
class NativeChrome {
    private class Entry {
        var backToken: Any? = null
        var back: (() -> Unit)? = null
        var backEnabled: Boolean = true
        var items: List<NativeBarItem> = emptyList()
        var onTap: (String) -> Unit = {}
    }

    private val entries = LinkedHashMap<Any, Entry>()
    private var last: NativeBar? = null

    /** Called with the bar in front whenever it changes, and once with the current one when set. */
    var onChange: ((NativeBar?) -> Unit)? = null
        set(value) {
            field = value
            last = current
            value?.invoke(last)
        }

    val current: NativeBar?
        get() = front()?.let { NativeBar(back = it.back != null, backEnabled = it.backEnabled, items = it.items) }

    private fun front(): Entry? = entries.values.lastOrNull()

    fun setBack(owner: Any, token: Any, onBack: () -> Unit, enabled: Boolean) {
        val entry = entries.getOrPut(owner) { Entry() }
        entry.backToken = token
        entry.back = onBack
        entry.backEnabled = enabled
        changed()
    }

    fun clearBack(owner: Any, token: Any) {
        val entry = entries[owner] ?: return
        if (entry.backToken != token) return
        entry.backToken = null
        entry.back = null
        drop(owner, entry)
    }

    fun setItems(owner: Any, items: List<NativeBarItem>, onTap: (String) -> Unit) {
        val entry = entries.getOrPut(owner) { Entry() }
        entry.items = items
        entry.onTap = onTap
        changed()
    }

    fun clearItems(owner: Any) {
        val entry = entries[owner] ?: return
        entry.items = emptyList()
        entry.onTap = {}
        drop(owner, entry)
    }

    /** A tap on an item of the bar in front. */
    fun tap(id: String) {
        val entry = front() ?: return
        if (entry.items.any { it.id == id && it.enabled }) entry.onTap(id)
    }

    /** The bar's back key. */
    fun back() {
        val entry = front() ?: return
        if (entry.backEnabled) entry.back?.invoke()
    }

    private fun drop(owner: Any, entry: Entry) {
        if (entry.back == null && entry.items.isEmpty()) entries.remove(owner)
        changed()
    }

    private fun changed() {
        val now = current
        if (now == last) return
        last = now
        onChange?.invoke(now)
    }
}

/** Non-null when the host shows the bars natively: screens then hand their back key and actions to it. */
val LocalNativeChrome = staticCompositionLocalOf<NativeChrome?> { null }

/** The screen the bar contributions below belong to; set once per navigation entry. */
val LocalNativeScreen = staticCompositionLocalOf<Any?> { null }

/** Hands the screen's back key to the native bar while composed. */
@Composable
fun NativeBack(chrome: NativeChrome, onBack: () -> Unit, enabled: Boolean) {
    val owner = LocalNativeScreen.current ?: remember { Any() }
    val token = remember { Any() }
    val latest by rememberUpdatedState(onBack)
    val call = remember { { latest() } }
    SideEffect { chrome.setBack(owner, token, call, enabled) }
    DisposableEffect(chrome, owner) { onDispose { chrome.clearBack(owner, token) } }
}

/**
 * Hands the screen's bar items to the native bar while composed. An [overlay] (a full-screen cover over the screen)
 * gets a bar of its own, in front of the screen's, for as long as it shows.
 */
@Composable
fun NativeItems(items: List<NativeBarItem>, overlay: Boolean = false, onTap: (String) -> Unit) {
    val chrome = LocalNativeChrome.current ?: return
    val own = remember { Any() }
    val owner = if (overlay) own else LocalNativeScreen.current ?: own
    val latest by rememberUpdatedState(onTap)
    val call = remember { { id: String -> latest(id) } }
    SideEffect { chrome.setItems(owner, items, call) }
    DisposableEffect(chrome, owner) { onDispose { chrome.clearItems(owner) } }
}
