package com.freya02.bot

import com.freya02.botcommands.api.core.ServiceStart
import com.freya02.botcommands.api.core.annotations.BService
import dev.minn.jda.ktx.jdabuilder.light
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.IEventManager
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag

@BService(ServiceStart.READY)
class JDAService(config: Config, manager: IEventManager) {
    init {
        light(config.token, enableCoroutines = false) {
            enableCache(CacheFlag.CLIENT_STATUS)
            enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)

            setMaxReconnectDelay(128)
            setActivity(Activity.watching("the docs"))
            setEventManager(manager)
        }
    }
}