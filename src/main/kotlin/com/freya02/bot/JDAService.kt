package com.freya02.bot

import dev.minn.jda.ktx.jdabuilder.light
import io.github.freya022.botcommands.api.core.service.ServiceStart
import io.github.freya022.botcommands.api.core.service.annotations.BService
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.IEventManager

@BService(ServiceStart.READY)
class JDAService(config: Config, manager: IEventManager) {
    init {
        light(config.token, enableCoroutines = false) {
            setMaxReconnectDelay(128)
            setActivity(Activity.customStatus("Reading the docs"))
            setEventManager(manager)
        }
    }
}
