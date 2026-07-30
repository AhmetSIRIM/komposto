package com.trendyol.design.compat.annotation

/**
 * Marks APIs in the `compat` module that exist as temporary platform workarounds
 * (not as long-term design-system components).
 *
 * These APIs may change signature or be removed once the underlying Compose / platform
 * issue is fixed. Call sites must opt in explicitly with `@OptIn(ExperimentalCompatApi::class)`.
 */
@RequiresOptIn(
    "This Komposto Compat API is an opt-in crash workaround and may change or be removed.",
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalCompatApi
