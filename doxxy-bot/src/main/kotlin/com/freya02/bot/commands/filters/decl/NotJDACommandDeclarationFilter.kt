package com.freya02.bot.commands.filters.decl

import com.freya02.bot.utils.Utils.isJDAGuild
import io.github.freya022.botcommands.api.commands.CommandPath
import io.github.freya022.botcommands.api.commands.application.CommandDeclarationFilter
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.entities.Guild

@BService
object NotJDACommandDeclarationFilter : CommandDeclarationFilter {
    override fun filter(guild: Guild, path: CommandPath, commandId: String?): Boolean = !guild.isJDAGuild()
}
