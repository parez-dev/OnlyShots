package app.onlyshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.onlyshots.core.designsystem.component.LocalNativeScreen

/**
 * A navigation destination's root, named for UI tests ([TestTags]); lays its screen out exactly as without it. It also
 * marks the screen as one owner of native bar contributions ([LocalNativeScreen]).
 */
@Composable
fun TaggedScreen(tag: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNativeScreen provides remember { Any() }) {
        Box(Modifier.fillMaxSize().testTag(tag), propagateMinConstraints = true) { content() }
    }
}
