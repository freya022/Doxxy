package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.BContext
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.CommandPath
import com.freya02.botcommands.api.application.slash.DefaultValueSupplier
import com.freya02.docs.DocSourceType
import net.dv8tion.jda.api.entities.Guild

abstract class BaseDocCommand : ApplicationCommand() {
    protected val docIndexMap: DocIndexMap = DocIndexMap

    override fun getDefaultValueSupplier(
        context: BContext, guild: Guild,
        commandId: String?, commandPath: CommandPath,
        optionName: String, parameterType: Class<*>
    ): DefaultValueSupplier? {
        if (optionName == "source_type") {
            //use subcommand as a default value
            when (commandPath.subname) {
                "botcommands" -> return DefaultValueSupplier { DocSourceType.BOT_COMMANDS }
                "jda" -> return DefaultValueSupplier { DocSourceType.JDA }
                "java" -> return DefaultValueSupplier { DocSourceType.JAVA }
            }
        }

        return super.getDefaultValueSupplier(context, guild, commandId, commandPath, optionName, parameterType)
    }
}