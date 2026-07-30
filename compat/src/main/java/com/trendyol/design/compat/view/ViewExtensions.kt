package com.trendyol.design.compat.view

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * Like [View.post], but skips [block] if [lifecycleOwner] is below [Lifecycle.State.CREATED]
 * when the runnable runs (avoids work after the host is destroyed).
 */
internal fun View.safePost(lifecycleOwner: LifecycleOwner, block: () -> Unit) {
    post {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            block()
        }
    }
}
