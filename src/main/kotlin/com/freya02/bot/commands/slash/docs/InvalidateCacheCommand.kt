package com.freya02.bot.commands.slash.docs

import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.annotations.Test
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData

@Command
class InvalidateCacheCommand : ApplicationCommand() {
    @Test
    @TopLevelSlashCommandData(scope = CommandScope.GUILD)
    @JDASlashCommand(name = "invalidate")
    fun onSlashInvalidate(event: GuildSlashEvent) {
        event.deferReply(true).queue()

        for (autocompleteName in CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
            event.context.invalidateAutocompleteCache(autocompleteName)
        }
        event.hook.sendMessage("Done").queue()
    }
}