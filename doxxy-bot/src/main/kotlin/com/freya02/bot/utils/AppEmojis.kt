package com.freya02.bot.utils

import io.github.freya022.botcommands.api.core.annotations.BEventListener
import io.github.freya022.botcommands.api.core.annotations.BEventListener.RunMode
import io.github.freya022.botcommands.api.core.events.PreFirstGatewayConnectEvent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.withResource
import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji

object AppEmojis {
    private val toLoad = arrayListOf<LoadRequest>()
    private val loaded = hashMapOf<String, ApplicationEmoji>()

    val hasImplementations by   registerEmoji("HasImplementations.png",   emojiName = "has_implementations")
    val hasOverrides by         registerEmoji("HasOverrides.png",         emojiName = "has_overrides")
    val hasOverriddenMethods by registerEmoji("implementingMethod.png",   emojiName = "has_overridden_methods")
    val hasSuperclasses by      registerEmoji("HasSuper.png",             emojiName = "has_superclasses")
    val hasSuperinterfaces by   registerEmoji("HasSuperinterfaces.png",   emojiName = "has_superinterfaces")

    val abstractClass by        registerEmoji("abstractClass_dark.png",   emojiName = "abstract_class")
    val annotation by           registerEmoji("annotationtype.png",       emojiName = "annotation")
    val `class` by              registerEmoji("class.png")
    val enum by                 registerEmoji("enum.png")
    val `interface` by          registerEmoji("interface_dark.png",       emojiName = "interface")

    val methodDeclaration by    registerEmoji("abstractMethod.png",       emojiName = "abstract_method")
    val methodDefinition by     registerEmoji("method.png")

    val sync by                 registerEmoji("sync.png",                 emojiName = "sync")

    private fun registerEmoji(assetName: String, emojiName: String = assetName.substringBefore('.')): Lazy<ApplicationEmoji> {
        toLoad += LoadRequest(assetName, emojiName)
        return lazy {
            // Can't be null, the bot would not start up if one wasn't found
            loaded.remove(emojiName)!!
        }
    }

    private data class LoadRequest(val assetName: String, val emojiName: String)

    @BService
    class AppEmojiLoader {

        @BEventListener(mode = RunMode.BLOCKING)
        fun onPreGatewayConnect(event: PreFirstGatewayConnectEvent) {
            val appEmojis = event.jda.retrieveApplicationEmojis().complete()

            for ((assetName, emojiName) in toLoad) {
                loaded[emojiName] = appEmojis.find { it.name == emojiName } ?: run {
                    val icon = withResource("/emojis/$assetName", Icon::from)
                    event.jda.createApplicationEmoji(emojiName, icon).complete()
                }
            }
        }
    }
}