package com.freya02.bot.commands.slash

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.CommandScope
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.annotations.Test
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import org.slf4j.LoggerFactory

@CommandMarker
class SlashLogging : ApplicationCommand() {
    private val loggerContext = (LoggerFactory.getILoggerFactory() as LoggerContext)

    @Test
    @JDASlashCommand(
        scope = CommandScope.GUILD,
        name = "logging",
        description = "Sets the logging level for a package/class"
    )
    fun onSlashLogging(
        event: GuildSlashEvent,
        @AppOption(autocomplete = LOGGER_NAME_AUTOCOMPLETE_NAME) loggerName: String,
        @AppOption logLevel: Level
    ) {
        val loggerList = loggerContext.loggerList
        when (val logger = loggerList.firstOrNull { it.name == loggerName }) {
            null -> event.reply_("Logger not found: $loggerName", ephemeral = true).queue()
            else -> {
                logger.level = logLevel
                event.reply_("Log level ${logLevel.levelStr} applied to ${logger.name}", ephemeral = true).queue()
            }
        }
    }

    @AutocompletionHandler(name = LOGGER_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
    fun onLoggerNameAutocomplete(event: CommandAutoCompleteInteractionEvent): List<String> {
        return loggerContext.loggerList.map { it.name }
    }

    companion object {
        private const val LOGGER_NAME_AUTOCOMPLETE_NAME = "SlashLogging: loggerNameAutocomplete"
    }
}