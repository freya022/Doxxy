package dev.freya02.doxxy.bot.commands.slash

import dev.freya02.doxxy.bot.versioning.github.UpdateCountdown
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import io.github.freya022.botcommands.api.BCInfo
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.core.BContext
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.TimeFormat
import java.lang.management.ManagementFactory
import kotlin.time.Duration.Companion.minutes

@Command
class SlashInfo(private val context: BContext) : ApplicationCommand() {
    private val combinedMemberCountCountdown = UpdateCountdown(5.minutes)
    private var combinedMemberCount: Int = 0

    @JDASlashCommand(name = "info", description = "Gives info on the bot")
    suspend fun onSlashInfo(event: GuildSlashEvent) {
        event.deferReply(true).queue()

        event.hook.sendMessage(MessageCreate {
            embed {
                author {
                    name = event.jda.selfUser.name
                    iconUrl = event.jda.selfUser.effectiveAvatarUrl
                }

                field {
                    name = "BotCommands version"

                    @Suppress("SENSELESS_COMPARISON") //Might be null depending on version used
                    value = when {
                        BCInfo.COMMIT_HASH != null -> "[${BCInfo.VERSION}](https://github.com/freya022/BotCommands/commit/${BCInfo.COMMIT_HASH})"
                        else -> "[${BCInfo.VERSION}](https://github.com/freya022/BotCommands)"
                    }

                    inline = false
                }

                field {
                    name = "JDA version"

                    @Suppress("SENSELESS_COMPARISON") //Might be null depending on version used
                    value = when {
                        JDAInfo.COMMIT_HASH == null || JDAInfo.COMMIT_HASH.endsWith("DEV") -> "[${JDAInfo.VERSION}](https://github.com/discord-jda/JDA)"
                        else -> "[${JDAInfo.VERSION}](https://github.com/discord-jda/JDA/commit/${JDAInfo.COMMIT_HASH})"
                    }

                    inline = true
                }

                field {
                    name = "Gateway version"
                    value = JDAInfo.DISCORD_GATEWAY_VERSION.toString()
                    inline = true
                }

                field {
                    name = "REST version"
                    value = JDAInfo.DISCORD_REST_VERSION.toString()
                    inline = true
                }

                field {
                    name = "Uptime"
                    value = TimeFormat.RELATIVE.format(ManagementFactory.getRuntimeMXBean().startTime)
                    inline = true
                }

                field {
                    name = "REST Ping"
                    value = "${event.jda.restPing.await()} ms"
                    inline = true
                }

                field {
                    name = "Gateway Ping"
                    value = "${event.jda.gatewayPing} ms"
                    inline = true
                }

                field {
                    name = "Guild"
                    value = "${event.jda.guildCache.size() + event.jda.unavailableGuilds.size}"
                    inline = true
                }

                field {
                    name = "Members"
                    value = "${getCombinedMemberCount()}"
                    inline = true
                }

                field {
                    name = "Memory usage"

                    val heapMemoryUsage = ManagementFactory.getMemoryMXBean().heapMemoryUsage
                    value = "%.2f / %d MB".format(heapMemoryUsage.used / 1024.0 / 1024.0, (heapMemoryUsage.max / 1024.0 / 1024.0).toInt())
                }
            }

            components += row(Button.link("https://github.com/freya022/Doxxy", "Source"))
        }).queue()
    }

    private suspend fun getCombinedMemberCount(): Int {
        combinedMemberCountCountdown.onUpdate {
            combinedMemberCount = context.jda.guilds
                .map { it.retrieveMetaData() }
                .let { RestAction.allOf(it) }
                .await()
                .sumOf { it.approximateMembers }
        }

        return combinedMemberCount
    }
}
