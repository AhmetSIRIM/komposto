package com.trendyol.design.compat.inputfield

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.InputType
import android.util.TypedValue
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density

/** Maps Compose [TextStyle] onto the EditText (color, size, weight; clears default padding/bg). */
internal fun AppCompatEditText.applyTextStyle(style: TextStyle, density: Density) {
    setTextColor(style.color.toArgb())
    val sizeSp = with(density) { style.fontSize.toPx() / density.density }
    if (!sizeSp.isNaN()) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }
    val weight = style.fontWeight?.weight ?: FontWeight.Normal.weight
    typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Typeface.create(typeface, weight, false)
    } else {
        Typeface.create(
            typeface,
            if (weight >= FontWeight.Bold.weight) Typeface.BOLD else Typeface.NORMAL,
        )
    }
    setPadding(0, 0, 0, 0)
    background = null
    includeFontPadding = false
}

/** Sets the text cursor drawable color (API 29+). */
internal fun AppCompatEditText.applyCursorColor(color: Color) {
    val drawable = GradientDrawable().apply {
        setColor(color.toArgb())
        setSize(resources.displayMetrics.density.toInt().coerceAtLeast(1), -1)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        textCursorDrawable = drawable
    }
}

/** Applies cursor color only when ARGB differs from the last applied value. */
internal fun SelectionAwareEditText.applyCursorColorIfChanged(color: Color) {
    val argb = color.toArgb()
    if (appliedCursorArgb == argb) return
    applyCursorColor(color)
    appliedCursorArgb = argb
}

/** Maps Compose [ImeAction] (+ single-line flag) to [EditorInfo] `imeOptions`. */
internal fun KeyboardOptions.toEditorInfoImeOptions(singleLine: Boolean): Int {
    var options = when (imeAction) {
        ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
        ImeAction.Done -> EditorInfo.IME_ACTION_DONE
        ImeAction.Go -> EditorInfo.IME_ACTION_GO
        ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
        ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
        ImeAction.Send -> EditorInfo.IME_ACTION_SEND
        else -> EditorInfo.IME_ACTION_UNSPECIFIED
    }
    if (singleLine) {
        options = options or EditorInfo.IME_FLAG_NO_FULLSCREEN
    }
    return options
}

/**
 * Maps Compose [KeyboardType] / capitalization / [singleLine] to Android [InputType] flags
 * (including multi-line for non-single-line text classes).
 */
@Suppress("CyclomaticComplexMethod")
internal fun KeyboardOptions.toInputType(singleLine: Boolean): Int {
    var type = when (keyboardType) {
        KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER
        KeyboardType.Decimal ->
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        KeyboardType.Phone -> InputType.TYPE_CLASS_PHONE
        KeyboardType.Email ->
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        KeyboardType.Password ->
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        KeyboardType.NumberPassword ->
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> InputType.TYPE_CLASS_TEXT
    }
    if (singleLine && keyboardType == KeyboardType.Text) {
        type = type or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    }
    if (singleLine.not() && (type and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT) {
        type = type or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    }
    when (capitalization) {
        KeyboardCapitalization.Characters ->
            type = type or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        KeyboardCapitalization.Words ->
            type = type or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        KeyboardCapitalization.Sentences ->
            type = type or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        else -> Unit
    }
    return type
}

/** Invokes [onAction] when the soft keyboard fires the matching editor action id. */
internal fun EditText.bindImeAction(imeAction: ImeAction, onAction: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        val expected = when (imeAction) {
            ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
            ImeAction.Done -> EditorInfo.IME_ACTION_DONE
            ImeAction.Go -> EditorInfo.IME_ACTION_GO
            ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
            ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
            ImeAction.Send -> EditorInfo.IME_ACTION_SEND
            else -> return@setOnEditorActionListener false
        }
        if (actionId == expected) {
            onAction()
            true
        } else {
            false
        }
    }
}

internal fun SolidColor.toColor(): Color = value
