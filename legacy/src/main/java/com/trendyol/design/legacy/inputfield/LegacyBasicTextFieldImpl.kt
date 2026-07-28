package com.trendyol.design.legacy.inputfield

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.viewinterop.AndroidView
import com.trendyol.design.legacy.R

/**
 * KeyboardActionScope used when dispatching [KeyboardActions] callbacks.
 *
 * [KeyboardActionScope.defaultKeyboardAction] is intentionally a no-op: platform default
 * IME focus traversal / hide-keyboard behavior is not forwarded through this bridge.
 */
private object NoOpKeyboardActionScope : KeyboardActionScope {
    override fun defaultKeyboardAction(imeAction: ImeAction) = Unit
}

@Composable
@Suppress("LongParameterList", "ModifierWithoutDefault")
internal fun LegacyBasicTextFieldImpl(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    readOnly: Boolean,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    maxLines: Int,
    visualTransformation: VisualTransformation,
    onTextLayout: (TextLayoutResult) -> Unit,
    interactionSource: MutableInteractionSource,
    cursorBrush: Brush,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val valueState = rememberUpdatedState(value)
    val keyboardActionsState = rememberUpdatedState(keyboardActions)
    val keyboardOptionsState = rememberUpdatedState(keyboardOptions)
    val interactionSourceState = rememberUpdatedState(interactionSource)
    val editTextRef = remember { EditTextRef() }
    val lastEmitted = remember { LastEmittedValue() }
    val editorProps = remember { EditorPropsSnapshot() }

    RememberFocusInteractionDispose(editTextRef, interactionSource)

    // AndroidView + EditText breaks Compose virtual clocks / hint LaunchedEffects under
    // Robolectric unit tests (LocalInspectionMode). Keep preview/test path on Foundation BTF.
    // onTextLayout is invoked only on this inspection path; AndroidView path does not synthesize
    // a TextLayoutResult.
    if (LocalInspectionMode.current) {
        BasicTextField(
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
        return
    }

    fun invokeKeyboardAction(imeAction: ImeAction) {
        val actions = keyboardActionsState.value
        val actionScope = NoOpKeyboardActionScope
        when (imeAction) {
            ImeAction.Search -> actions.onSearch?.invoke(actionScope)
            ImeAction.Done -> actions.onDone?.invoke(actionScope)
            ImeAction.Go -> actions.onGo?.invoke(actionScope)
            ImeAction.Next -> actions.onNext?.invoke(actionScope)
            ImeAction.Previous -> actions.onPrevious?.invoke(actionScope)
            ImeAction.Send -> actions.onSend?.invoke(actionScope)
            else -> Unit
        }
    }

    decorationBox {
        AndroidView(
            modifier = modifier.legacyEditTextSemantics(
                valueState = valueState,
                editTextRef = editTextRef,
                enabled = enabled,
                readOnly = readOnly,
                imeAction = keyboardOptions.imeAction,
                lastEmitted = lastEmitted,
                onValueChangeState = onValueChangeState,
                onImeAction = { invokeKeyboardAction(keyboardOptionsState.value.imeAction) },
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
            ),
            factory = { context ->
                SelectionAwareEditText(context).apply {
                    id = R.id.kp_legacy_basic_text_field
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )

                    applyTextStyleIfChanged(editorProps, textStyle, density, force = true)
                    applyEditorProps(
                        snapshot = editorProps,
                        enabled = enabled,
                        readOnly = readOnly,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        keyboardOptions = keyboardOptions,
                        force = true,
                    )
                    applyVisualTransformation(visualTransformation, readOnly = readOnly)
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    minimumHeight = 0
                    setPadding(0, 0, 0, 0)
                    if (cursorBrush is SolidColor) {
                        applyCursorColorIfChanged(cursorBrush.toColor())
                    }
                    setOnClickListener {
                        requestFocusAndShowKeyboard()
                    }

                    bindTextAndSelectionListeners(lastEmitted, onValueChangeState)
                    bindFocusInteractions(scope, interactionSourceState)

                    suppressingState.suppressing = true
                    try {
                        setText(value.text)
                        setSelection(
                            value.selection.min.coerceIn(0, value.text.length),
                            value.selection.max.coerceIn(0, value.text.length),
                        )
                        lastEmitted.syncFrom(value)
                    } finally {
                        suppressingState.suppressing = false
                    }

                    bindImeActionIfChanged(editorProps, keyboardOptions.imeAction) {
                        invokeKeyboardAction(keyboardOptionsState.value.imeAction)
                    }
                    editTextRef.editText = this
                }
            },
            update = { editText ->
                editTextRef.editText = editText
                editText.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                editText.applyEditorProps(
                    snapshot = editorProps,
                    enabled = enabled,
                    readOnly = readOnly,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                )

                editText.applyVisualTransformation(visualTransformation, readOnly = readOnly)
                editText.applyTextStyleIfChanged(editorProps, textStyle, density)
                if (cursorBrush is SolidColor) {
                    editText.applyCursorColorIfChanged(cursorBrush.toColor())
                }
                editText.bindImeActionIfChanged(editorProps, keyboardOptions.imeAction) {
                    invokeKeyboardAction(keyboardOptionsState.value.imeAction)
                }
                editText.applyValueFromCompose(valueState.value, lastEmitted)
            },
        )
    }
}
