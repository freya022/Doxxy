package com.freya02.bot.commands.text

import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.annotations.RequireOwner
import com.freya02.botcommands.api.commands.prefixed.BaseCommandEvent
import com.freya02.botcommands.api.commands.prefixed.TextCommand
import com.freya02.botcommands.api.commands.prefixed.annotations.JDATextCommand
import net.dv8tion.jda.api.entities.ClientType

@CommandMarker
class Test : TextCommand() {
    @RequireOwner
    @JDATextCommand(name = "test")
    fun test(event: BaseCommandEvent) {
        println(event.member.onlineStatus)
        println(event.member.getOnlineStatus(ClientType.DESKTOP))
        println(event.member.getOnlineStatus(ClientType.MOBILE))
        println(event.member.getOnlineStatus(ClientType.WEB))
    }
}