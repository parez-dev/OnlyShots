package app.onlyshots

import app.onlyshots.core.designsystem.component.BarIcon
import app.onlyshots.core.designsystem.component.NativeBar
import app.onlyshots.core.designsystem.component.NativeBarItem
import app.onlyshots.core.designsystem.component.NativeChrome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NativeChromeTest {
    private val export = NativeBarItem("export", "Export", BarIcon.Export, prominent = true)
    private val help = NativeBarItem("help", "Help", BarIcon.Help)

    @Test
    fun theLastScreenRegisteredIsInFrontAndPoppingItBringsTheOneBelowBack() {
        val chrome = NativeChrome()
        val home = Any()
        val edit = Any()
        chrome.setBack(home, Any(), {}, enabled = true)
        chrome.setItems(home, listOf(help)) {}
        chrome.setItems(edit, listOf(export)) {}
        assertEquals(NativeBar(back = false, backEnabled = true, items = listOf(export)), chrome.current)

        chrome.clearItems(edit)
        assertEquals(NativeBar(back = true, backEnabled = true, items = listOf(help)), chrome.current)
    }

    @Test
    fun anUpdateKeepsTheScreensPlaceSoAnOverlayStaysInFront() {
        val chrome = NativeChrome()
        val screen = Any()
        val overlay = Any()
        chrome.setItems(screen, listOf(help)) {}
        chrome.setItems(overlay, listOf(export)) {}
        chrome.setItems(screen, listOf(help.copy(enabled = false))) {}
        assertEquals(listOf(export), chrome.current?.items)
    }

    @Test
    fun tapsAndBackReachOnlyTheScreenInFrontAndNotWhenDisabled() {
        val chrome = NativeChrome()
        val taps = mutableListOf<String>()
        var backs = 0
        val below = Any()
        val front = Any()
        chrome.setItems(below, listOf(help)) { taps += "below:$it" }
        chrome.setItems(front, listOf(export, help.copy(enabled = false))) { taps += "front:$it" }
        chrome.setBack(front, Any(), { backs++ }, enabled = false)

        chrome.tap("export")
        chrome.tap("help")
        chrome.back()
        assertEquals(listOf("front:export"), taps)
        assertEquals(0, backs)

        chrome.setBack(front, Any(), { backs++ }, enabled = true)
        chrome.back()
        assertEquals(1, backs)
    }

    @Test
    fun aStaleBackKeyLeavingDoesNotClearTheNewOne() {
        val chrome = NativeChrome()
        val screen = Any()
        val old = Any()
        val new = Any()
        chrome.setBack(screen, old, {}, enabled = true)
        chrome.setBack(screen, new, {}, enabled = true)
        chrome.clearBack(screen, old)
        assertEquals(true, chrome.current?.back)
        chrome.clearBack(screen, new)
        assertNull(chrome.current)
    }

    @Test
    fun listenersHearOnlyChanges() {
        val chrome = NativeChrome()
        val heard = mutableListOf<NativeBar?>()
        chrome.onChange = { heard += it }
        val screen = Any()
        chrome.setItems(screen, listOf(help)) {}
        chrome.setItems(screen, listOf(help)) {}
        assertEquals(listOf(null, NativeBar(false, true, listOf(help))), heard)
    }
}
