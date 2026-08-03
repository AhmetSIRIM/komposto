package com.trendyol.design.compat.inputfield

import androidx.annotation.IdRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.trendyol.design.compat.R
import com.trendyol.design.compat.annotation.ExperimentalCompatApi
import com.trendyol.theme.KPDesign

/**
 * Drop-in alternative to Compose [androidx.compose.foundation.text.BasicTextField] that renders
 * text with a platform [android.widget.EditText] inside [androidx.compose.ui.viewinterop.AndroidView].
 *
 * ## Why this exists
 * Compose Foundation selection / [android.view.textclassifier.TextClassifier] can crash on some
 * devices (rapid typing + double-tap select-all). See
 * [issuetracker.google.com/issues/515272359](https://issuetracker.google.com/issues/515272359).
 * This component keeps the familiar BasicTextField API but moves selection to the platform widget.
 *
 * ## When to use
 * Only as a crash workaround. Prefer Foundation [androidx.compose.foundation.text.BasicTextField]
 * (or Komposto styled inputs) everywhere else — this is **not** a standard design-system text field.
 *
 * ## Migrating from BasicTextField
 * Most parameters match BasicTextField. Important differences:
 * - Apply [modifier] focus helpers (`focusRequester`, `onFocusChanged`, `testTag`) on this composable.
 *   The embedded editor is a **focus group** — use [androidx.compose.ui.focus.FocusState.hasFocus],
 *   not `isFocused`.
 * - Width is caller-owned (no forced `fillMaxWidth()`).
 * - [keyboardActions]: only your lambdas run. Platform defaults (hide IME / focus next) are **not**
 *   forwarded via [androidx.compose.foundation.text.KeyboardActionScope.defaultKeyboardAction].
 *   Soft-keyboard IME buttons and hardware / emulator Enter both invoke the matching callback.
 * - [visualTransformation] is bridged through Android `TransformationMethod`. Prefer
 *   `remember { PasswordVisualTransformation() }` so the same instance survives recomposition.
 * - [onTextLayout] runs only in preview / inspection (`LocalInspectionMode`); not on device
 *   AndroidView path.
 * - [cursorBrush]: only [SolidColor] is applied.
 * - [viewId]: Android `EditText` resource id used by Maestro / UIAutomator `id:` and by tooling that
 *   resolves `Resources.getResourceEntryName(view.id)`. Defaults to [R.id.kp_compat_basic_text_field].
 *   Pass a caller-owned id (e.g. `R.id.editTextSearchView`) when the screen needs a unique id.
 *   Do **not** use [android.view.View.NO_ID] — some app tooling crashes on `#0xffffffff`.
 *
 * For caret / selection state, use the [TextFieldValue] overload.
 */
@ExperimentalCompatApi
@Composable
public fun KPCompatBasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = KPDesign.typography.subtitleMedium,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(KPDesign.colors.colorPrimary),
    @IdRes viewId: Int = R.id.kp_compat_basic_text_field,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    val textFieldValue = textFieldValueState.copy(text = value)
    SideEffect {
        if (
            textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }
    var lastTextValue by remember(value) { mutableStateOf(value) }

    KPCompatBasicTextField(
        value = textFieldValue,
        onValueChange = { newTextFieldValueState ->
            textFieldValueState = newTextFieldValueState
            val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
            lastTextValue = newTextFieldValueState.text
            if (stringChangedSinceLastInvocation) {
                onValueChange(newTextFieldValueState.text)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        viewId = viewId,
        decorationBox = decorationBox,
    )
}

/**
 * [TextFieldValue] overload of [KPCompatBasicTextField].
 *
 * Same purpose and migration notes as the `String` overload. Additional state caveats:
 * - IME composing region ([TextFieldValue.composition]) is **not** bridged from EditText.
 * - While focused, programmatic **selection-only** updates are ignored (platform ActionMode owns
 *   the caret). Change [TextFieldValue.text], or move the cursor via semantics `setSelection`.
 *
 * Focus / Unfocus [androidx.compose.foundation.interaction.FocusInteraction]s are emitted on
 * [interactionSource] from the embedded EditText.
 */
@ExperimentalCompatApi
@Composable
public fun KPCompatBasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = KPDesign.typography.subtitleMedium,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(KPDesign.colors.colorPrimary),
    @IdRes viewId: Int = R.id.kp_compat_basic_text_field,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    CompatBasicTextFieldImpl(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        viewId = viewId,
        decorationBox = decorationBox,
    )
}
