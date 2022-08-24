package com.freya02.bot.commands.slash.docs

import com.freya02.botcommands.api.commands.CommandPath
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.slash.ApplicationGeneratedValueSupplier
import com.freya02.botcommands.api.parameters.ParameterType
import com.freya02.docs.DocSourceType
import net.dv8tion.jda.api.entities.Guild

abstract class BaseDocCommand : ApplicationCommand() {
    override fun getGeneratedValueSupplier(
        guild: Guild?,
        commandId: String?,
        commandPath: CommandPath,
        optionName: String,
        parameterType: ParameterType
    ): ApplicationGeneratedValueSupplier {
        if (optionName == "source_type") {
            //use subcommand as a default value
            when (commandPath.subname) {
                "botcommands" -> return ApplicationGeneratedValueSupplier { DocSourceType.BOT_COMMANDS }
                "jda" -> return ApplicationGeneratedValueSupplier { DocSourceType.JDA }
                "java" -> return ApplicationGeneratedValueSupplier { DocSourceType.JAVA }
            }
        }

        return super.getGeneratedValueSupplier(guild, commandId, commandPath, optionName, parameterType)
    }
}