package com.trendyol.design.legacy.inputfield

import android.graphics.Rect
import android.text.method.TransformationMethod
import android.view.View
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Bridges Compose [VisualTransformation] to Android [TransformationMethod].
 *
 * Length-preserving transforms (e.g. [androidx.compose.ui.text.input.PasswordVisualTransformation])
 * work like Compose BasicTextField. Asymmetric [androidx.compose.ui.text.input.OffsetMapping]
 * is best-effort: EditText selection stays on the original [android.text.Editable], while
 * display uses the transformed CharSequence.
 */
internal class LegacyVisualTransformationMethod(
    private var visualTransformation: VisualTransformation,
) : TransformationMethod {

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
