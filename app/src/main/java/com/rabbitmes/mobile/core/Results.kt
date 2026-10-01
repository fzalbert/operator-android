package com.rabbitmes.mobile.core

import com.rabbitmes.mobile.domain.*
import kotlinx.coroutines.CancellationException

/**
 * Как [runCatching], но не перехватывает [CancellationException]:
 * иначе отменённая корутина продолжит работу и будет писать в стейт.
 */
internal inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        Result.failure(error)
    }
