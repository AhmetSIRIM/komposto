package com.trendyol.design.compat.inputfield

import android.content.Context
import android.graphics.Rect
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Job

/**
 * When `true`, text/selection listeners must not call [onValueChange] — Compose is pushing state
 * into the view and a listener echo would feedback-loop.
 */
internal class SuppressingState {
    var suppressing: Boolean = false
}

/**
 * Mutable holder for the live [SelectionAwareEditText].
 *
 * Kept outside Compose state so assigning the view after [androidx.compose.ui.viewinterop.AndroidView]
 * factory/update does not trigger recomposition.
 */
internal class EditTextRef {
    var editText: SelectionAwareEditText? = null
}

/**
 * Deduplicates view → Compose emissions.
 *
 * Text watchers and selection callbacks can fire for the same logical change; this keeps
 * [TextFieldValue] delivery at most once per distinct text+selection pair.
 */
internal class LastEmittedValue {
    private var text: String? = null
    private var selStart: Int = -1
    private var selEnd: Int = -1

    fun shouldEmit(text: String, selStart: Int, selEnd: Int): Boolean {
        if (this.text == text && this.selStart == selStart && this.selEnd == selEnd) {
            return false
        }
        this.text = text
        this.selStart = selStart
        this.selEnd = selEnd
        return true
    }

    fun syncFrom(value: TextFieldValue) {
        text = value.text
        selStart = value.selection.min
        selEnd = value.selection.max
    }
}

/**
 * [AppCompatEditText] that surfaces selection-only changes via [onSelectionChange]
 * (text changes still go through `doAfterTextChanged`).
 *
 * Also stores bridge fields used across sync / focus / visual-transformation helpers
 * (suppress flag, VT method, focus interaction bookkeeping, cursor color cache).
 */
internal class SelectionAwareEditText(context: Context) : AppCompatEditText(context) {
    val suppressingState = SuppressingState()
    var onSelectionChange: ((selStart: Int, selEnd: Int) -> Unit)? = null
    var visualTransformationMethod: CompatVisualTransformationMethod? = null
    var appliedVisualTransformationFingerprint: String? = null
    var focusedInteraction: FocusInteraction.Focus? = null
    var focusEmitJob: Job? = null
    var appliedCursorArgb: Int? = null
    var keyboardLifecycleOwner: LifecycleOwner? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChange?.invoke(selStart, selEnd)
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        val result = super.requestFocus(direction, previouslyFocusedRect)
        val owner = keyboardLifecycleOwner
        if (hasFocus() && owner != null) {
            showSoftInputIfAble(owner)
        }
        return result
    }
}
