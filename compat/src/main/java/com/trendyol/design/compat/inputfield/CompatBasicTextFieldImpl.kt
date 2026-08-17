package com.trendyol.design.compat.inputfield

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope

/**
 * [KeyboardActionScope] passed into [KeyboardActions] lambdas.
 *
 * [KeyboardActionScope.defaultKeyboardAction] is a no-op: hide-IME / focus-next defaults are not
 * forwarded from the platform EditText bridge.
 */
private object NoOpKeyboardActionScope : KeyboardActionScope {
    override fun defaultKeyboardAction(imeAction: ImeAction) = Unit
}

/**
 * Internal host for [KPCompatBasicTextField].
 *
 * - **Inspection / preview** ([LocalInspectionMode]): delegates to Foundation [BasicTextField]
 *   so Compose tooling and [onTextLayout] keep working.
 * - **Device**: [AndroidView] + [SelectionAwareEditText], with change-gated prop sync and
 *   Compose ↔ view value bridging.
 */
@Composable
@Suppress("LongParameterList", "ModifierWithoutDefault")
internal fun CompatBasicTextFieldImpl(
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
    viewId: Int,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val scope = rememberCoroutineScope()
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val valueState = rememberUpdatedState(value)
    val keyboardActionsState = rememberUpdatedState(keyboardActions)
    val keyboardOptionsState = rememberUpdatedState(keyboardOptions)
    val interactionSourceState = rememberUpdatedState(interactionSource)
    val lifecycleOwnerState = rememberUpdatedState(LocalLifecycleOwner.current)
    val editTextRef = remember { EditTextRef() }
    val lastEmitted = remember { LastEmittedValue() }
    val editorProps = remember { EditorPropsSnapshot() }

    RememberFocusInteractionDispose(editTextRef, interactionSource)

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
        dispatchCompatKeyboardAction(
            imeAction = imeAction,
            editText = editTextRef.editText,
            lifecycleOwner = lifecycleOwnerState.value,
            keyboardActions = keyboardActionsState.value,
        )
    }

    decorationBox {
        AndroidView(
            modifier = modifier
                .compatShowImeOnComposeFocus(editTextRef) { lifecycleOwnerState.value }
                .compatEditTextSemantics(
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
                    lifecycleOwner = lifecycleOwnerState.value,
                ),
            factory = { context ->
                SelectionAwareEditText(context).also { editText ->
                    editText.bindCompatFactory(
                        viewId = viewId,
                        value = value,
                        enabled = enabled,
                        readOnly = readOnly,
                        textStyle = textStyle,
                        keyboardOptions = keyboardOptions,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        visualTransformation = visualTransformation,
                        cursorBrush = cursorBrush,
                        density = density,
                        fontFamilyResolver = fontFamilyResolver,
                        editorProps = editorProps,
                        lastEmitted = lastEmitted,
                        onValueChangeState = onValueChangeState,
                        scope = scope,
                        interactionSourceState = interactionSourceState,
                        lifecycleOwnerState = lifecycleOwnerState,
                        onImeAction = {
                            invokeKeyboardAction(keyboardOptionsState.value.imeAction)
                        },
                    )
                    editTextRef.editText = editText
                }
            },
            update = { editText ->
                editTextRef.editText = editText
                editText.syncCompatUpdate(
                    viewId = viewId,
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = textStyle,
                    keyboardOptions = keyboardOptions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    visualTransformation = visualTransformation,
                    cursorBrush = cursorBrush,
                    density = density,
                    fontFamilyResolver = fontFamilyResolver,
                    editorProps = editorProps,
                    value = valueState.value,
                    lastEmitted = lastEmitted,
                    lifecycleOwner = lifecycleOwnerState.value,
                    onImeAction = {
                        invokeKeyboardAction(keyboardOptionsState.value.imeAction)
                    },
                )
            },
            onRelease = { editText -> releaseCompatEditText(editText, editTextRef) },
        )
    }
}

private fun dispatchCompatKeyboardAction(
    imeAction: ImeAction,
    editText: SelectionAwareEditText?,
    lifecycleOwner: LifecycleOwner,
    keyboardActions: KeyboardActions,
) {
    if (
        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED).not() ||
        editText == null ||
        editText.isAttachedToWindow.not()
    ) {
        return
    }
    val actionScope = NoOpKeyboardActionScope
    when (imeAction) {
        ImeAction.Search -> keyboardActions.onSearch?.invoke(actionScope)
        ImeAction.Done -> keyboardActions.onDone?.invoke(actionScope)
        ImeAction.Go -> keyboardActions.onGo?.invoke(actionScope)
        ImeAction.Next -> keyboardActions.onNext?.invoke(actionScope)
        ImeAction.Previous -> keyboardActions.onPrevious?.invoke(actionScope)
        ImeAction.Send -> keyboardActions.onSend?.invoke(actionScope)
        else -> Unit
    }
}

@Suppress("LongParameterList")
private fun SelectionAwareEditText.bindCompatFactory(
    viewId: Int,
    value: TextFieldValue,
    enabled: Boolean,
    readOnly: Boolean,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions,
    singleLine: Boolean,
    maxLines: Int,
    visualTransformation: VisualTransformation,
    cursorBrush: Brush,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    editorProps: EditorPropsSnapshot,
    lastEmitted: LastEmittedValue,
    onValueChangeState: State<(TextFieldValue) -> Unit>,
    scope: CoroutineScope,
    interactionSourceState: State<MutableInteractionSource>,
    lifecycleOwnerState: State<LifecycleOwner>,
    onImeAction: () -> Unit,
) {
    id = viewId
    layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
    keyboardLifecycleOwner = lifecycleOwnerState.value
    applyTextStyleIfChanged(
        snapshot = editorProps,
        style = textStyle,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
        force = true,
    )
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
    applyCursorBrushIfSolid(cursorBrush)
    setOnClickListener {
        requestFocusAndShowKeyboard(lifecycleOwnerState.value)
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
    bindImeActionIfChanged(editorProps, keyboardOptions.imeAction, onImeAction)
}

@Suppress("LongParameterList")
private fun SelectionAwareEditText.syncCompatUpdate(
    viewId: Int,
    enabled: Boolean,
    readOnly: Boolean,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions,
    singleLine: Boolean,
    maxLines: Int,
    visualTransformation: VisualTransformation,
    cursorBrush: Brush,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    editorProps: EditorPropsSnapshot,
    value: TextFieldValue,
    lastEmitted: LastEmittedValue,
    lifecycleOwner: LifecycleOwner,
    onImeAction: () -> Unit,
) {
    keyboardLifecycleOwner = lifecycleOwner
    if (id != viewId) {
        id = viewId
    }
    layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )
    applyEditorProps(
        snapshot = editorProps,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
    )
    applyVisualTransformation(visualTransformation, readOnly = readOnly)
    applyTextStyleIfChanged(
        snapshot = editorProps,
        style = textStyle,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
    )
    applyCursorBrushIfSolid(cursorBrush)
    bindImeActionIfChanged(editorProps, keyboardOptions.imeAction, onImeAction)
    val wasNonEmpty = text?.isNotEmpty() == true
    applyValueFromCompose(value, lastEmitted)
    if (wasNonEmpty && value.text.isEmpty() && hasFocus()) {
        requestFocusAndShowKeyboard(lifecycleOwner)
    }
}

private fun SelectionAwareEditText.applyCursorBrushIfSolid(cursorBrush: Brush) {
    if (cursorBrush is SolidColor) {
        applyCursorColorIfChanged(cursorBrush.toColor())
    }
}

private fun releaseCompatEditText(
    editText: SelectionAwareEditText,
    editTextRef: EditTextRef,
) {
    editText.setOnEditorActionListener(null)
    editText.keyboardLifecycleOwner = null
    if (editTextRef.editText === editText) {
        editTextRef.editText = null
    }
}
