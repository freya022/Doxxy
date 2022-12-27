package com.freya02.bot.docs

import com.freya02.bot.db.Database
import com.freya02.botcommands.api.core.ServiceStart
import com.freya02.botcommands.api.core.annotations.BService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@BService(start = ServiceStart.LAZY)
class DocMentionRepository(private val database: Database) {
    private val lock = Mutex()

    suspend fun markUsed(messageId: Long, userId: Long): Unit = lock.withLock {
        database.preparedStatement("insert into doc_mention (message_id, user_id) VALUES (?, ?)") {
            executeUpdate(messageId, userId)
        }
    }

    suspend fun hasBeenUsed(messageId: Long, userId: Long): Boolean = lock.withLock {
        database.preparedStatement("select * from doc_mention where message_id = ? and user_id = ? limit 1") {
            return@preparedStatement executeQuery(messageId, userId).readOnce() != null
        }
    }
}