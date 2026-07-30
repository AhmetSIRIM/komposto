@file:Suppress("MatchingDeclarationName")

package com.trendyol.design.compat.inputfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density

/**
 * Snapshot of last-applied EditText editor props (enabled, inputType, IME, style caches).
 *
 * [AndroidView] `update` runs every recomposition; comparing against this snapshot avoids
 * rewriting `inputType` / listeners when nothing meaningful changed.
 */
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

/**
 * Read-only = `keyListener = null` (keeps focus/selection). Clearing read-only relies on
 * re-assigning [android.widget.TextView.setInputType], which restores the default KeyListener.
 */
internal fun SelectionAwareEditText.applyReadOnly(readOnly: Boolean) {
    if (readOnly) {
        keyListener = null
    }
}

/**
 * Applies enabled / lines / [InputType] / IME options when they differ from [snapshot]
 * (or when [force] is true on first bind).
 */
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

/** Applies [style] only when color / size / weight differ from [snapshot] (or [force]). */
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

/** Rebinds the editor-action listener only when [imeAction] changes. */
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
 * Installs Compose [visualTransformation] as an Android [android.text.method.TransformationMethod].
 *
 * Must run **after** [applyEditorProps]: password `inputType` can install a platform
 * TransformationMethod that we then override.
 *
 * Rebind is gated on [visualTransformationFingerprint] (behavior), not instance identity, so
 * `PasswordVisualTransformation()` allocated each recomposition does not thrash. When the
 * fingerprint changes, clear-then-set drops EditText's cached transformed CharSequence
 * (`setTransformationMethod` no-ops on the same instance).
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
        ?: CompatVisualTransformationMethod(visualTransformation).also {
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
 * Stable identity for a [VisualTransformation] config across recompositions.
 *
 * Runs [filter] on a fixed probe (`"Aa1!"` — letter/digit/symbol) and pairs the class name with
 * the transformed output. Same mask ⇒ same fingerprint even when the VT instance is new each frame.
 */
internal fun VisualTransformation.visualTransformationFingerprint(): String {
    if (this === VisualTransformation.None) return "None"
    val sample = "Aa1!"
    val transformed = filter(AnnotatedString(sample)).text.text
    return "${this::class.java.name}|$transformed"
}
