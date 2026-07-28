package com.trendyol.design.legacy.inputfield

import android.content.Context
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Job

internal class SuppressingState {
    var suppressing: Boolean = false
}

/** Holds the platform EditText without triggering recomposition on assign. */
internal class EditTextRef {
    var editText: SelectionAwareEditText? = null
}

/**
 * Coalesces text-watcher + selection-listener emissions so identical
 * TextFieldValue is delivered at most once per logical change.
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
 * EditText that reports selection-only changes (text changes still go through doAfterTextChanged).
 */
internal class SelectionAwareEditText(context: Context) : AppCompatEditText(context) {
    val suppressingState = SuppressingState()
    var onSelectionChange: ((selStart: Int, selEnd: Int) -> Unit)? = null
    var visualTransformationMethod: LegacyVisualTransformationMethod? = null
    var appliedVisualTransformationFingerprint: String? = null
    var focusedInteraction: FocusInteraction.Focus? = null
    var focusEmitJob: Job? = null
    var appliedCursorArgb: Int? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChange?.invoke(selStart, selEnd)
    }
}
