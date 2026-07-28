package com.trendyol.design.legacy.inputfield

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.onImeAction
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions

internal fun AppCompatEditText.requestFocusAndShowKeyboard() {
    if (isEnabled.not() || isFocusable.not()) return
    requestFocus()
    if (keyListener == null) return
    post {
        if (hasFocus().not()) return@post
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

internal fun isPasswordSemantics(
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
): Boolean {
    if (visualTransformation is PasswordVisualTransformation ||
        keyboardOptions.keyboardType == KeyboardType.Password ||
        keyboardOptions.keyboardType == KeyboardType.NumberPassword
    ) {
        return true
    }

    if (visualTransformation === VisualTransformation.None) return false
    val sample = "Ab1!"
    val transformed = visualTransformation.filter(AnnotatedString(sample)).text.text
    return transformed.length == sample.length &&
        transformed != sample &&
        transformed.isNotEmpty() &&
        transformed.all { it == transformed.first() }
}

@Suppress("LongParameterList")
internal fun Modifier.legacyEditTextSemantics(
    valueState: State<TextFieldValue>,
    editTextRef: EditTextRef,
    enabled: Boolean,
    readOnly: Boolean,
    imeAction: ImeAction,
    lastEmitted: LastEmittedValue,
    onValueChangeState: State<(TextFieldValue) -> Unit>,
    onImeAction: () -> Unit,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
): Modifier = semantics(mergeDescendants = true) {
    val current = valueState.value
    if (isPasswordSemantics(visualTransformation, keyboardOptions)) {
        password()
    }
    editableText = AnnotatedString(current.text)
    textSelectionRange = current.selection
    requestFocus {
        val editText = editTextRef.editText
        if (enabled.not() || editText == null) return@requestFocus false
        editText.requestFocusAndShowKeyboard()
        editText.hasFocus()
    }
    setText {
        if (enabled.not() || readOnly) return@setText false
        editTextRef.editText?.applyTextFromSemantics(
            newText = it.text,
            lastEmitted = lastEmitted,
            onValueChange = onValueChangeState.value,
        ) ?: return@setText false
        true
    }
    insertTextAtCursor { text ->
        if (enabled.not() || readOnly) return@insertTextAtCursor false
        editTextRef.editText?.insertTextFromSemantics(
            text = text,
            lastEmitted = lastEmitted,
            onValueChange = onValueChangeState.value,
        ) ?: return@insertTextAtCursor false
        true
    }
    setSelection { start, end, _ ->
        if (enabled.not()) return@setSelection false
        val et = editTextRef.editText ?: return@setSelection false
        val len = et.text?.length ?: 0
        et.suppressingState.suppressing = true
        try {
            et.setSelection(start.coerceIn(0, len), end.coerceIn(0, len))
        } finally {
            et.suppressingState.suppressing = false
        }
        et.emitValueChangeFromView(lastEmitted, onValueChangeState.value)
        true
    }
    onImeAction(imeAction) {
        onImeAction()
        true
    }
}
