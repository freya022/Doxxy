package com.freya02.bot.docs.mentions

import com.freya02.botcommands.api.core.db.Database
import com.freya02.botcommands.api.core.db.KConnection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DocMentionRepository(private val database: Database) {
    private val lock = Mutex()

    suspend fun ifNotUsed(messageId: Long, userId: Long, block: suspend () -> Unit) {
        lock.withLock {
            database.withConnection {
                if (hasBeenUsed(messageId, userId)) return
                markUsed(messageId, userId)
            }
        }

        //Don't hold the lock and connection while outer code is executing
        block()
    }

    context(KConnection)
    private suspend fun markUsed(messageId: Long, userId: Long) {
        preparedStatement("insert into doc_mention (message_id, user_id) VALUES (?, ?)") {
            executeUpdate(messageId, userId)
        }
    }

    context(KConnection)
    private suspend fun hasBeenUsed(messageId: Long, userId: Long): Boolean {
        preparedStatement("select * from doc_mention where message_id = ? and user_id = ? limit 1") {
            return executeQuery(messageId, userId).readOnce() != null
        }
    }
}