package com.freya02.bot.docs

import com.freya02.bot.docs.index.DocIndex
import com.freya02.botcommands.api.Logging
import com.freya02.docs.DocSourceType
import java.io.IOException
import java.util.*


object DocIndexMap : EnumMap<DocSourceType, DocIndex>(DocSourceType::class.java) {
    private val LOGGER = Logging.getLogger()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                for (index in values) {
                    index.close()
                }
            } catch (e: IOException) {
                LOGGER.error("Unable to close cache", e)
            }
        })

        this[DocSourceType.BOT_COMMANDS] = DocIndex(DocSourceType.BOT_COMMANDS)
        this[DocSourceType.JDA] = DocIndex(DocSourceType.JDA)
        this[DocSourceType.JAVA] = DocIndex(DocSourceType.JAVA)
    }

    @Synchronized
    @Throws(IOException::class)
    fun refreshAndInvalidateIndex(sourceType: DocSourceType) {
        this[sourceType]?.reindex()
    }
}