package com.trendyol.design.compat.inputfield

import android.graphics.Rect
import android.text.method.TransformationMethod
import android.view.View
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Adapts Compose [VisualTransformation] to Android [TransformationMethod] for EditText.
 *
 * Length-preserving transforms (e.g. password mask) match BasicTextField. Asymmetric
 * [androidx.compose.ui.text.input.OffsetMapping] is best-effort: selection stays on the
 * underlying [android.text.Editable]; only the displayed CharSequence is transformed.
 */
internal class CompatVisualTransformationMethod(
    private var visualTransformation: VisualTransformation,
) : TransformationMethod {

    /** Swap the Compose VT without allocating a new [TransformationMethod] instance. */
    fun update(visualTransformation: VisualTransformation) {
        this.visualTransformation = visualTransformation
    }

    override fun getTransformation(source: CharSequence?, view: View?): CharSequence {
        val text = source ?: return ""
        if (visualTransformation === VisualTransformation.None) return text
        return visualTransformation.filter(AnnotatedString(text.toString())).text
    }

    override fun onFocusChanged(
        view: View?,
        sourceText: CharSequence?,
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?,
    ) = Unit
}
