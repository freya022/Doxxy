package com.freya02.bot.commands.text

import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.annotations.RequireOwner
import io.github.freya022.botcommands.api.commands.text.BaseCommandEvent
import io.github.freya022.botcommands.api.commands.text.TextCommand
import io.github.freya022.botcommands.api.commands.text.annotations.JDATextCommand
import net.dv8tion.jda.api.entities.ClientType

@Command
class Test : TextCommand() {
    @RequireOwner
    @JDATextCommand(path = ["test"])
    fun test(event: BaseCommandEvent) {
        println(event.member.onlineStatus)
        println(event.member.getOnlineStatus(ClientType.DESKTOP))
        println(event.member.getOnlineStatus(ClientType.MOBILE))
        println(event.member.getOnlineStatus(ClientType.WEB))
    }
}