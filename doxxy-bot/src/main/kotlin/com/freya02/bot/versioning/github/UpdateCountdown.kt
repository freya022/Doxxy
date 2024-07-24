package com.freya02.bot.versioning.github

import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

class UpdateCountdown(duration: Duration) {
    private val interval: Long = duration.inWholeMilliseconds
    private var nextUpdate: Long = 0

    suspend fun onUpdate(block: suspend () -> Unit) {
        if (System.currentTimeMillis() > nextUpdate) {
            try {
                block()
            } finally {
                nextUpdate = System.currentTimeMillis() + interval
            }
        }
    }
}

class UpdateCountdownDelegate<T : Any>(duration: Duration, private val updater: () -> T) : ReadOnlyProperty<Any, T> {
    private val countdown = UpdateCountdown(duration)
    private lateinit var value: T

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        runBlocking {
            countdown.onUpdate {
                value = updater()
            }
        }

        return value
    }
}