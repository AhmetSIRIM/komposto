package com.trendyol.design.compat.inputfield

import androidx.compose.runtime.State
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.widget.doAfterTextChanged

/**
 * View → Compose: build a [TextFieldValue] from current text + selection and emit if new.
 *
 * [TextFieldValue.composition] is never set — bridging IME composing spans would need
 * InputConnection hooks and is intentionally omitted.
 */
internal fun SelectionAwareEditText.emitValueChangeFromView(
    lastEmitted: LastEmittedValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val text = this.text?.toString().orEmpty()
    val start = selectionStart.coerceIn(0, text.length)
    val end = selectionEnd.coerceIn(0, text.length)
    if (lastEmitted.shouldEmit(text, start, end).not()) return

    onValueChange(TextFieldValue(text = text, selection = TextRange(start, end)))
}

/** Accessibility / semantics `setText`: replace contents and move caret to end. */
internal fun SelectionAwareEditText.applyTextFromSemantics(
    newText: String,
    lastEmitted: LastEmittedValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    suppressingState.suppressing = true
    try {
        setText(newText)
        setSelection(newText.length)
    } finally {
        suppressingState.suppressing = false
    }
    emitValueChangeFromView(lastEmitted, onValueChange)
}

/** Accessibility / semantics `insertTextAtCursor`: replace the current selection. */
internal fun SelectionAwareEditText.insertTextFromSemantics(
    text: AnnotatedString,
    lastEmitted: LastEmittedValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val editable = editableText ?: return
    val start = selectionStart.coerceIn(0, editable.length)
    val end = selectionEnd.coerceIn(0, editable.length)
    val plain = text.text
    suppressingState.suppressing = true
    try {
        editable.replace(minOf(start, end), maxOf(start, end), plain)
        val cursor = minOf(start, end) + plain.length
        setSelection(cursor.coerceIn(0, editable.length))
    } finally {
        suppressingState.suppressing = false
    }
    emitValueChangeFromView(lastEmitted, onValueChange)
}

/**
 * Compose → view: apply incoming [TextFieldValue] without listener echo.
 *
 * While focused, **selection-only** updates are skipped so platform ActionMode / caret
 * ownership is not fighting Compose. Text changes always apply; selection applies when
 * unfocused or when text also changed.
 */
internal fun SelectionAwareEditText.applyValueFromCompose(
    incoming: TextFieldValue,
    lastEmitted: LastEmittedValue,
) {
    val current = text?.toString().orEmpty()
    val sameText = current == incoming.text
    val sameSelection = sameText &&
        selectionStart == incoming.selection.min &&
        selectionEnd == incoming.selection.max
    if (sameSelection) {
        lastEmitted.syncFrom(incoming)
        return
    }

    suppressingState.suppressing = true
    try {
        if (!sameText) {
            setText(incoming.text)
            val len = text?.length ?: 0
            setSelection(
                incoming.selection.min.coerceIn(0, len),
                incoming.selection.max.coerceIn(0, len),
            )
            lastEmitted.syncFrom(incoming)
        } else if (!hasFocus()) {
            val len = text?.length ?: 0
            setSelection(
                incoming.selection.min.coerceIn(0, len),
                incoming.selection.max.coerceIn(0, len),
            )
            lastEmitted.syncFrom(incoming)
        }
    } finally {
        suppressingState.suppressing = false
    }
}

/**
 * Wires text + selection listeners that push [TextFieldValue] into Compose.
 * No-ops while [SuppressingState.suppressing] is true.
 */
internal fun SelectionAwareEditText.bindTextAndSelectionListeners(
    lastEmitted: LastEmittedValue,
    onValueChangeState: State<(TextFieldValue) -> Unit>,
) {
    doAfterTextChanged { editable ->
        if (suppressingState.suppressing) return@doAfterTextChanged
        val text = editable?.toString().orEmpty()
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(0, text.length)
        if (!lastEmitted.shouldEmit(text, start, end)) return@doAfterTextChanged
        onValueChangeState.value(
            TextFieldValue(text = text, selection = TextRange(start, end)),
        )
    }

    onSelectionChange = { selStart, selEnd ->
        if (!suppressingState.suppressing) {
            val text = this.text?.toString().orEmpty()
            val start = selStart.coerceIn(0, text.length)
            val end = selEnd.coerceIn(0, text.length)
            if (lastEmitted.shouldEmit(text, start, end)) {
                onValueChangeState.value(
                    TextFieldValue(text = text, selection = TextRange(start, end)),
                )
            }
        }
    }
}
