package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.annotations.Test
import com.freya02.botcommands.api.commands.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand

@Command
class InvalidateCacheCommand(private val docIndexMap: DocIndexMap) : ApplicationCommand() {
    @Test
    @JDASlashCommand(scope = CommandScope.GUILD, name = "invalidate")
    fun onSlashInvalidate(event: GuildSlashEvent) {
        event.deferReply(true).queue()

        for (autocompleteName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
            event.context.invalidateAutocompleteCache(autocompleteName)
        }
        event.hook.sendMessage("Done").queue()
    }
}