package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.annotations.Test
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand

@CommandMarker
class InvalidateCacheCommand(private val docIndexMap: DocIndexMap) : ApplicationCommand() {
    @Test
    @JDASlashCommand(name = "invalidate")
    fun onSlashInvalidate(event: GuildSlashEvent) {
        event.deferReply(true).queue()

        for (autocompleteName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
            event.context.invalidateAutocompletionCache(autocompleteName)
        }
        event.hook.sendMessage("Done").queue()
    }
}