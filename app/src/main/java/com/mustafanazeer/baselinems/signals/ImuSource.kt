package com.mustafanazeer.baselinems.signals

import com.mustafanazeer.baselinems.dsp.ImuSample
import kotlinx.coroutines.flow.Flow

/**
 * Signals layer contract for the rest of the application: a typed, lifecycle managed stream
 * of `ImuSample` values that the gait pipeline consumes.
 *
 * Implementations are expected to register sensor listeners on `start()`, emit samples
 * through `stream()` until `stop()` is called, and unregister on `stop()`. The interface
 * does not commit to thread or scheduler choice; consumers wrap it in a coroutine if they
 * need cancellation cooperative behavior.
 */
interface ImuSource {
    fun start()
    fun stop()
    fun stream(): Flow<ImuSample>
}
