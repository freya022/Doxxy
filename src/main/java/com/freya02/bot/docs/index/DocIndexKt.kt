package com.freya02.bot.docs.index

import com.freya02.bot.db.Database
import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import com.freya02.botcommands.api.Logging
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocsSession
import com.freya02.docs2.PageCache
import org.slf4j.Logger

private val LOGGER: Logger = Logging.getLogger()

//Initial construct just allows database access
// Further updates must be invoked by external methods such as version checkers
class DocIndexKt(private val sourceType: DocSourceType, private val database: Database) : IDocIndexKt {
    fun getClassDoc(className: String): CachedClass? {
        TODO("Not yet implemented")
    }

    override fun getMethodDoc(className: String, methodId: String): CachedMethod? {
        TODO("Not yet implemented")
    }

    override fun getFieldDoc(className: String, fieldName: String): CachedField? {
        TODO("Not yet implemented")
    }

    override fun getMethodDocSuggestions(className: String): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getMethodDocSuggestions(): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getFieldDocSuggestions(className: String): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getFieldDocSuggestions(): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getMethodAndFieldDocSuggestions(className: String): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getSimpleNameList(): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getClassesWithMethods(): Collection<String> {
        TODO("Not yet implemented")
    }

    override fun getClassesWithFields(): Collection<String> {
        TODO("Not yet implemented")
    }

    fun reindex(): DocIndexKt {
        LOGGER.info("Re-indexing docs for {}", sourceType.name)

        LOGGER.info("Clearing cache for {}", sourceType.name)
        PageCache.clearCache(sourceType)
        LOGGER.info("Cleared cache of {}", sourceType.name)

        val docsSession = DocsSession()

        DocIndexWriter(database, docsSession, sourceType).doReindex()

        LOGGER.info("Re-indexed docs for {}", sourceType.name)

        System.gc() //Very effective

        return this
    }
}