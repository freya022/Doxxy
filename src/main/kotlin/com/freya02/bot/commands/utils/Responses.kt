package com.freya02.bot.commands.utils

import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData

fun MessageCreateData.toEditData() =
    MessageEditData.fromCreateData(this)
fun MessageEditData.edit(callback: IMessageEditCallback) =
    callback.editMessage(this)