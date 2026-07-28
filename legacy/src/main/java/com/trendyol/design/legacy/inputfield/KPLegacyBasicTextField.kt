package com.trendyol.design.legacy.inputfield

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
import com.trendyol.design.legacy.annotation.ExperimentalLegacyApi
import com.trendyol.theme.KPDesign

/**
 * Platform [android.widget.EditText]-backed text field with a Compose
 * [androidx.compose.foundation.text.BasicTextField]-like API.
 *
 * Use this when Compose Foundation text selection / TextClassifier crashes
 * (e.g. rapid type + double-tap select-all). Selection is handled by the platform widget.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text.
 * An updated text comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field (focusRequester, testTag should be applied here).
 * AndroidView exposes the embedded editor as a focus group; use FocusState.hasFocus in
 * onFocusChanged callbacks. Width is caller-owned (no forced fillMaxWidth).
 * @param enabled controls the enabled state of the text field
 * @param readOnly controls the editable state of the text field
 * @param textStyle the style to be applied to the input text
 * @param keyboardOptions software keyboard options that contains configuration
 * @param keyboardActions when the input service emits an IME action, the corresponding callback.
 * Default platform IME actions (hide keyboard / focus next) are not forwarded; only the
 * provided [KeyboardActions] lambdas run.
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines
 * @param maxLines the maximum height in terms of maximum number of visible lines
 * @param visualTransformation transforms the visual representation of the input [value]
 * via Android [android.text.method.TransformationMethod]. Length-preserving transforms
 * (e.g. password mask) match Compose; asymmetric OffsetMapping is best-effort.
 * Prefer `remember { PasswordVisualTransformation() }` (or any custom VT) so the same
 * instance is reused across recompositions.
 * @param onTextLayout callback for text layout. Invoked only in LocalInspectionMode
 * (Foundation BasicTextField path); not invoked on the AndroidView path.
 * @param interactionSource the [MutableInteractionSource] representing interactions.
 * Focus/unfocus interactions are emitted from the embedded EditText.
 * @param cursorBrush [Brush] to paint cursor with. Only [SolidColor] is applied
 * @param decorationBox composable lambda that allows to add decorations around text field,
 * such as icon, placeholder, helper messages or similar
 */
@ExperimentalLegacyApi
@Composable
public fun KPLegacyBasicTextField(
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
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    // Mirror Compose BasicTextField(String) selection bridging so cursor is not reset on recomposition.
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

    KPLegacyBasicTextField(
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
        decorationBox = decorationBox,
    )
}

/**
 * Platform [android.widget.EditText]-backed text field with a Compose
 * [androidx.compose.foundation.text.BasicTextField]-like API.
 *
 * Use this when Compose Foundation text selection / TextClassifier crashes
 * (e.g. rapid type + double-tap select-all). Selection is handled by the platform widget.
 *
 * @param value the [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 * this text field. An updated [TextFieldValue] comes as a parameter of the callback.
 * IME composing region ([TextFieldValue.composition]) is not bridged from EditText.
 * While the field is focused, programmatic [TextFieldValue.selection]-only updates are not
 * applied (platform selection / ActionMode owns the caret); change [TextFieldValue.text]
 * or use semantics setSelection to move the cursor from Compose.
 * @param modifier a [Modifier] for this text field (focusRequester, testTag should be applied here).
 * AndroidView exposes the embedded editor as a focus group; use FocusState.hasFocus in
 * onFocusChanged callbacks. Width is caller-owned (no forced fillMaxWidth).
 * @param enabled controls the enabled state of the text field
 * @param readOnly controls the editable state of the text field
 * @param textStyle the style to be applied to the input text
 * @param keyboardOptions software keyboard options that contains configuration
 * @param keyboardActions when the input service emits an IME action, the corresponding callback.
 * Default platform IME actions (hide keyboard / focus next) are not forwarded; only the
 * provided [KeyboardActions] lambdas run.
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines
 * @param maxLines the maximum height in terms of maximum number of visible lines
 * @param visualTransformation transforms the visual representation of the input [value]
 * via Android [android.text.method.TransformationMethod]. Length-preserving transforms
 * (e.g. password mask) match Compose; asymmetric OffsetMapping is best-effort.
 * Prefer `remember { PasswordVisualTransformation() }` (or any custom VT) so the same
 * instance is reused across recompositions.
 * @param onTextLayout callback for text layout. Invoked only in LocalInspectionMode
 * (Foundation BasicTextField path); not invoked on the AndroidView path.
 * @param interactionSource the [MutableInteractionSource] representing interactions.
 * Focus/unfocus interactions are emitted from the embedded EditText.
 * @param cursorBrush [Brush] to paint cursor with. Only [SolidColor] is applied
 * @param decorationBox composable lambda that allows to add decorations around text field,
 * such as icon, placeholder, helper messages or similar
 */
@ExperimentalLegacyApi
@Composable
public fun KPLegacyBasicTextField(
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
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    LegacyBasicTextFieldImpl(
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
        decorationBox = decorationBox,
    )
}
