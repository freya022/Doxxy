package com.freya02.bot.commands.slash.docs

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers.Companion.SEARCH_AUTOCOMPLETE_NAME
import com.freya02.bot.commands.slash.docs.controllers.SlashDocsController
import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandManager
import io.github.freya022.botcommands.api.commands.application.provider.GuildApplicationCommandProvider

@Command
class SlashSearch : GuildApplicationCommandProvider {
    override fun declareGuildApplicationCommands(manager: GuildApplicationCommandManager) {
        manager.slashCommand("search", CommandScope.GUILD, null) {
            description = "Searches the documentation for classes, methods and fields"

            DocSourceType.typesForGuild(manager.guild).forEach { sourceType ->
                subcommand(sourceType.cmdName, SlashDocsController::onSearchSlashCommand) {
                    description = "Searches the documentation for classes, methods and fields"

                    generatedOption("sourceType") { sourceType }

                    option("query") {
                        description = "Search query, # only searches methods, all uppercase for constant fields"
                        autocompleteByName(SEARCH_AUTOCOMPLETE_NAME)
                    }

                    //TODO add option to replace description with link
                }
            }
        }
    }
}
