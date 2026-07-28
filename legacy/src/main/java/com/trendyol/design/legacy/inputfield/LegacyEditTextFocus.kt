package com.trendyol.design.legacy.inputfield

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun SelectionAwareEditText.bindFocusInteractions(
    scope: CoroutineScope,
    interactionSourceState: State<MutableInteractionSource>,
) {
    setOnFocusChangeListener { _, hasFocus ->
        focusEmitJob?.cancel()
        focusEmitJob = scope.launch {
            val interactionSource = interactionSourceState.value
            if (hasFocus) {
                val focus = FocusInteraction.Focus()
                focusedInteraction = focus
                interactionSource.emit(focus)
            } else {
                focusedInteraction?.let { focus ->
                    interactionSource.emit(FocusInteraction.Unfocus(focus))
                }
                focusedInteraction = null
            }
        }
    }
}

/**
 * Ensures a matching [FocusInteraction.Unfocus] is emitted if the composable leaves composition
 * while the EditText is still focused (navigate-away / dispose).
 *
 * Cancels any in-flight focus emit job first so a late [FocusInteraction.Focus] cannot
 * land after [FocusInteraction.Unfocus].
 */
@Composable
internal fun RememberFocusInteractionDispose(
    editTextRef: EditTextRef,
    interactionSource: MutableInteractionSource,
) {
    val interactionSourceState = rememberUpdatedState(interactionSource)
    DisposableEffect(editTextRef) {
        onDispose {
            val editText = editTextRef.editText ?: return@onDispose
            editText.focusEmitJob?.cancel()
            editText.focusEmitJob = null
            val focus = editText.focusedInteraction ?: return@onDispose
            editText.focusedInteraction = null
            interactionSourceState.value.tryEmit(FocusInteraction.Unfocus(focus))
        }
    }
}
