package com.freya02.bot.versioning.github

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

open class UpdateCountdown(duration: Duration) {
    private val interval: Long = duration.inWholeMilliseconds
    private var nextUpdate: Long = 0

    fun needsUpdate(): Boolean = when {
        System.currentTimeMillis() > nextUpdate -> {
            nextUpdate = System.currentTimeMillis() + interval
            true
        }
        else -> false
    }
}

class UpdateCountdownDelegate<T : Any>(duration: Duration, private val updater: () -> T): UpdateCountdown(duration), ReadOnlyProperty<Any, T> {
    private lateinit var value: T

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (needsUpdate()) {
            value = updater()
        }

        return value
    }
}