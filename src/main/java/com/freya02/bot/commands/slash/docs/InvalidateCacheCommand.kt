package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.annotations.Test
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.docs.DocSourceType

@CommandMarker
class InvalidateCacheCommand : ApplicationCommand() {
    @Test
    @JDASlashCommand(name = "invalidate")
    fun onSlashInvalidate(event: GuildSlashEvent) {
        event.deferReply(true).queue()
        DocIndexMap.refreshAndInvalidateIndex(DocSourceType.BOT_COMMANDS)
        DocIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA)
        //		DocIndexMap.refreshAndInvalidateIndex(DocSourceType.JAVA); Java's docs dont get updated lol

        for (autocompleteName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
            event.context.invalidateAutocompletionCache(autocompleteName)
        }
        event.hook.sendMessage("Done").queue()
    }
}