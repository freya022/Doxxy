package com.freya02.bot.utils

import net.dv8tion.jda.internal.entities.ReceivedMessage

inline fun <R> suppressContentWarning(block: () -> R): R {
    val oldFlag = ReceivedMessage.didContentIntentWarning
    ReceivedMessage.didContentIntentWarning = true

    return try {
        block()
    } finally {
        ReceivedMessage.didContentIntentWarning = oldFlag
    }
}