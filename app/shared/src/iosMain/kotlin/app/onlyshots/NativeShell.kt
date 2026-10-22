package app.onlyshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import app.onlyshots.core.designsystem.component.NativeBar
import app.onlyshots.core.designsystem.component.NativeChrome
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** The Swift host's side of the native bar: told the bar in front, as JSON, whenever it changes. */
interface NativeShellListener {
    fun barChanged(json: String)
}

/**
 * The bridge between the screens' bar contributions ([NativeChrome]) and the Swift host's `UINavigationController`
 * (iPhone Duo, iOS 27.1): the host listens, shows the bar natively and hands taps back. Until the host turns it on (it
 * does once it sees a hinge), the screens draw their own bars as on any other phone.
 */
object NativeShell {
    private val chrome = NativeChrome()
    private val enabled = mutableStateOf(false)

    var listener: NativeShellListener? = null
        set(value) {
            field = value
            chrome.onChange = value?.let { host -> { bar -> host.barChanged(encode(bar)) } }
        }

    /** On where the system shows the bars (a foldable iPhone), off elsewhere; the screens follow at once. */
    fun setEnabled(on: Boolean) {
        enabled.value = on
    }

    fun tapped(id: String) = chrome.tap(id)

    fun back() = chrome.back()

    internal val current: NativeChrome?
        @Composable get() = if (enabled.value) chrome else null

    internal fun encode(bar: NativeBar?): String = buildJsonObject {
        put("back", bar?.back ?: false)
        put("backEnabled", bar?.backEnabled ?: true)
        putJsonArray("items") {
            for (item in bar?.items.orEmpty()) {
                addJsonObject {
                    put("id", item.id)
                    put("title", item.title)
                    put("icon", item.icon.name)
                    put("prominent", item.prominent)
                    put("enabled", item.enabled)
                    put("group", item.group)
                    put("bottom", item.bottom)
                    put("priority", item.priority.name)
                }
            }
        }
    }
        .toString()
}
