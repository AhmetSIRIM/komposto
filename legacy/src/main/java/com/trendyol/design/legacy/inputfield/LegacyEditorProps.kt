@file:Suppress("MatchingDeclarationName")

package com.trendyol.design.legacy.inputfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density

/** Tracks last applied editor props so update does not rewrite inputType every frame. */
internal class EditorPropsSnapshot {
    var applied: Boolean = false
    var enabled: Boolean = true
    var readOnly: Boolean = false
    var singleLine: Boolean = false
    var maxLines: Int = Int.MAX_VALUE
    var inputType: Int = 0
    var imeOptions: Int = 0
    var boundImeAction: ImeAction? = null
    var textColorArgb: Int = 0
    var fontSizeSp: Float = Float.NaN
    var fontWeight: Int = -1

    @Suppress("LongParameterList")
    fun matches(
        enabled: Boolean,
        readOnly: Boolean,
        singleLine: Boolean,
        maxLines: Int,
        inputType: Int,
        imeOptions: Int,
    ): Boolean {
        if (!applied) return false
        return this.enabled == enabled &&
            this.readOnly == readOnly &&
            this.singleLine == singleLine &&
            this.maxLines == maxLines &&
            this.inputType == inputType &&
            this.imeOptions == imeOptions
    }

    @Suppress("LongParameterList")
    fun capture(
        enabled: Boolean,
        readOnly: Boolean,
        singleLine: Boolean,
        maxLines: Int,
        inputType: Int,
        imeOptions: Int,
    ) {
        this.enabled = enabled
        this.readOnly = readOnly
        this.singleLine = singleLine
        this.maxLines = maxLines
        this.inputType = inputType
        this.imeOptions = imeOptions
        applied = true
    }

    fun styleMatches(style: TextStyle, density: Density): Boolean {
        if (!applied) return false
        val sizeSp = with(density) { style.fontSize.toPx() / density.density }
        val weight = style.fontWeight?.weight ?: FontWeight.Normal.weight
        return textColorArgb == style.color.toArgb() &&
            fontSizeSp == sizeSp &&
            fontWeight == weight
    }

    fun captureStyle(style: TextStyle, density: Density) {
        textColorArgb = style.color.toArgb()
        fontSizeSp = with(density) { style.fontSize.toPx() / density.density }
        fontWeight = style.fontWeight?.weight ?: FontWeight.Normal.weight
    }
}

internal fun SelectionAwareEditText.applyReadOnly(readOnly: Boolean) {
    // inputType restores the default KeyListener; null blocks typing while keeping focus/selection.
    if (readOnly) {
        keyListener = null
    }
}

@Suppress("LongParameterList")
internal fun SelectionAwareEditText.applyEditorProps(
    snapshot: EditorPropsSnapshot,
    enabled: Boolean,
    readOnly: Boolean,
    singleLine: Boolean,
    maxLines: Int,
    keyboardOptions: KeyboardOptions,
    force: Boolean = false,
) {
    val resolvedMaxLines = if (singleLine) 1 else maxLines
    val newInputType = keyboardOptions.toInputType(singleLine)
    val newImeOptions = keyboardOptions.toEditorInfoImeOptions(singleLine)
    if (
        force.not() &&
        snapshot.matches(
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = resolvedMaxLines,
            inputType = newInputType,
            imeOptions = newImeOptions,
        )
    ) {
        return
    }

    applyEnabledAndLineConstraints(
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = resolvedMaxLines,
    )
    applyInputTypeAndReadOnly(
        snapshot = snapshot,
        readOnly = readOnly,
        newInputType = newInputType,
        newImeOptions = newImeOptions,
    )
    snapshot.capture(
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = resolvedMaxLines,
        inputType = newInputType,
        imeOptions = newImeOptions,
    )
}

private fun SelectionAwareEditText.applyEnabledAndLineConstraints(
    enabled: Boolean,
    readOnly: Boolean,
    singleLine: Boolean,
    maxLines: Int,
) {
    isEnabled = enabled
    isFocusable = enabled
    isFocusableInTouchMode = enabled
    isCursorVisible = enabled && !readOnly
    isSingleLine = singleLine
    this.maxLines = maxLines
}

private fun SelectionAwareEditText.applyInputTypeAndReadOnly(
    snapshot: EditorPropsSnapshot,
    readOnly: Boolean,
    newInputType: Int,
    newImeOptions: Int,
) {
    val inputTypeChanged = snapshot.applied.not() || snapshot.inputType != newInputType
    val readOnlyChanged = snapshot.applied.not() || snapshot.readOnly != readOnly
    val restoreKeyListener = readOnlyChanged && readOnly.not() && snapshot.applied

    if (inputTypeChanged || restoreKeyListener) {
        inputType = newInputType
    }
    if (snapshot.applied.not() || snapshot.imeOptions != newImeOptions) {
        imeOptions = newImeOptions
    }
    if (inputTypeChanged || readOnlyChanged) {
        applyReadOnly(readOnly)
    }
}

internal fun SelectionAwareEditText.applyTextStyleIfChanged(
    snapshot: EditorPropsSnapshot,
    style: TextStyle,
    density: Density,
    force: Boolean = false,
) {
    if (!force && snapshot.styleMatches(style, density)) return
    applyTextStyle(style, density)
    snapshot.captureStyle(style, density)
}

internal fun SelectionAwareEditText.bindImeActionIfChanged(
    snapshot: EditorPropsSnapshot,
    imeAction: ImeAction,
    onAction: () -> Unit,
) {
    if (snapshot.boundImeAction == imeAction) return
    bindImeAction(imeAction, onAction)
    snapshot.boundImeAction = imeAction
}

/**
 * Applies [visualTransformation] after editor props: password [android.widget.TextView.setInputType]
 * may install a platform TransformationMethod that we override when Compose VT is set.
 *
 * Cache bust is gated on a stable [visualTransformationFingerprint], not instance identity, so
 * `PasswordVisualTransformation()` recreated each recomposition does not null/re-set every frame.
 * Prefer `remember { PasswordVisualTransformation() }` at call sites anyway.
 *
 * [android.widget.TextView.setTransformationMethod] no-ops when the same instance is passed again,
 * so when the fingerprint changes we clear then re-assign to drop the cached transformed CharSequence.
 */
internal fun SelectionAwareEditText.applyVisualTransformation(
    visualTransformation: VisualTransformation,
    readOnly: Boolean,
) {
    val fingerprint = visualTransformation.visualTransformationFingerprint()
    if (visualTransformation === VisualTransformation.None) {
        if (visualTransformationMethod != null && transformationMethod === visualTransformationMethod) {
            transformationMethod = null
            inputType = inputType
            applyReadOnly(readOnly)
        }
        visualTransformationMethod = null
        appliedVisualTransformationFingerprint = fingerprint
        return
    }
    val method = visualTransformationMethod
        ?: LegacyVisualTransformationMethod(visualTransformation).also {
            visualTransformationMethod = it
        }
    val fingerprintChanged = appliedVisualTransformationFingerprint != fingerprint
    method.update(visualTransformation)
    if (transformationMethod !== method) {
        transformationMethod = method
    } else if (fingerprintChanged) {
        transformationMethod = null
        transformationMethod = method
    }
    appliedVisualTransformationFingerprint = fingerprint
}

/**
 * Stable key for VT config. Equal PasswordVisualTransformation instances (same mask) share a
 * fingerprint even when allocated per recomposition.
 */
internal fun VisualTransformation.visualTransformationFingerprint(): String {
    if (this === VisualTransformation.None) return "None"
    val sample = "Aa1!"
    val transformed = filter(AnnotatedString(sample)).text.text
    return "${this::class.java.name}|$transformed"
}
