package app.onlyshots.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.onlyshots.TestTags
import app.onlyshots.core.designsystem.CgColors
import app.onlyshots.core.designsystem.CgIcons
import app.onlyshots.core.designsystem.CgMotion
import app.onlyshots.core.designsystem.CgShape
import app.onlyshots.core.designsystem.CgSize
import app.onlyshots.core.designsystem.CgSpace
import app.onlyshots.core.designsystem.CgStroke
import app.onlyshots.core.designsystem.CgTheme
import app.onlyshots.core.designsystem.caps
import app.onlyshots.core.resources.Res
import app.onlyshots.core.resources.core_back
import app.onlyshots.core.resources.core_record
import app.onlyshots.core.resources.core_stop
import org.jetbrains.compose.resources.stringResource

enum class CgButtonKind {
    /** A light bone key: the usual way forward. */
    Primary,
    /** A hairline outline: the other way. */
    Secondary,
    /** The signal: the one action a screen is for (export, save, open the camera, open the editor). */
    Signal,
}

/**
 * Figma "Button": a square key with a Mono label in capitals (callers pass the sentence-case string; the key sets it in
 * capitals). [Pill] is its compact sibling.
 */
@Composable
fun CgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: CgButtonKind = CgButtonKind.Primary,
    enabled: Boolean = true,
) {
    val bg =
        when {
            !enabled || kind == CgButtonKind.Secondary -> Color.Transparent
            kind == CgButtonKind.Signal -> CgColors.Brand.Signal
            else -> CgColors.Brand.Bone
        }
    val fg =
        when {
            !enabled -> CgColors.Text.Disabled
            kind == CgButtonKind.Secondary -> CgColors.Text.Primary
            else -> CgColors.Text.OnLight
        }
    val edge = if (!enabled || kind == CgButtonKind.Secondary) CgColors.Line.Hairline else Color.Transparent
    val bgColor by animateColorAsState(bg, CgMotion.short(), label = "bg")
    val fgColor by animateColorAsState(fg, CgMotion.short(), label = "fg")
    Box(
        modifier
            .defaultMinSize(minHeight = 52.dp, minWidth = CgSize.Touch)
            .background(bgColor, CgShape.Pill)
            .border(CgStroke.Hairline, edge, CgShape.Pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = CgSpace.S16, vertical = CgSpace.S12),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.caps(), style = CgTheme.type.label, color = fgColor, textAlign = TextAlign.Center)
    }
}

/** Figma "Round button": the 44 dp square key over the camera and in headers. */
@Composable
fun CgRoundButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .size(CgSize.Touch)
            .background(if (selected) CgColors.Brand.Bone else CgColors.Surface.Base.copy(alpha = 0.85f))
            .border(CgStroke.Hairline, if (selected) CgColors.Brand.Bone else CgColors.Line.Hairline)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The one back button: a 44 dp square key with a hairline edge, on every screen that has a way back. Where the host
 * shows the bars natively (iPhone Duo) it draws nothing and hands its action to the system bar instead.
 */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val native = LocalNativeChrome.current
    if (native != null) {
        NativeBack(native, onClick, enabled)
        return
    }
    val back = stringResource(Res.string.core_back)
    Box(
        modifier
            .testTag(TestTags.Back)
            .size(CgSize.Touch)
            .background(CgColors.Surface.Base.copy(alpha = 0.85f))
            .border(CgStroke.Hairline, CgColors.Line.Hairline)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = back },
        contentAlignment = Alignment.Center,
    ) {
        // Inert (mid-take), the chevron goes grey: still there, not to be pressed.
        CgIcon(
            CgIcons.ChevronLeft,
            size = 20.dp,
            tint = if (enabled) CgColors.Text.Primary else CgColors.Text.Secondary,
        )
    }
}

enum class RecordButtonState {
    Idle,
    Armed,
    Recording,
    Disabled,
}

/**
 * Figma "Record button": an 80 dp square frame around the red dot, which squares off into the stop key while recording.
 * Armed, the frame is the signal and blinks once a second, like a standby light; idle and recording it is bone;
 * disabled, a hairline with a grey dot.
 */
@Composable
fun RecordButton(
    state: RecordButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label =
        stringResource(if (state == RecordButtonState.Recording) Res.string.core_stop else Res.string.core_record)
    // Read while drawing, so the blinking redraws the frame without recomposing the button.
    val frameAlpha: () -> Float =
        if (state == RecordButtonState.Armed) {
            val pulse = CgMotion.rememberBlink(1000, dim = 0.2f)
            pulse::value
        } else {
            NoPulse
        }
    val enabled = state != RecordButtonState.Disabled
    val recording = state == RecordButtonState.Recording
    // Record and stop morph: the red dot squares off into the stop key, and back, and stops dead.
    val innerSize by animateDpAsState(if (recording) 30.dp else 32.dp, CgMotion.short(), label = "inner")
    val corner by animateDpAsState(if (recording) 0.dp else 16.dp, CgMotion.short(), label = "corner")
    val fill by
        animateColorAsState(
            if (enabled) CgColors.Status.Record else CgColors.Line.Hairline,
            CgMotion.short(),
            label = "fill",
        )
    val frame by
        animateColorAsState(
            when (state) {
                RecordButtonState.Armed -> CgColors.Brand.Signal
                RecordButtonState.Disabled -> CgColors.Line.Hairline
                else -> CgColors.Brand.Bone
            },
            CgMotion.short(),
            label = "frame",
        )
    Box(
        modifier
            .size(CgSize.Record)
            .background(CgColors.Surface.Base.copy(alpha = 0.6f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(CgSize.Record).graphicsLayer { alpha = frameAlpha() }.border(CgStroke.Rule, frame))
        Box(Modifier.size(innerSize).background(fill, RoundedCornerShape(corner.coerceAtLeast(0.dp))))
    }
}

private val NoPulse: () -> Float = { 1f }
