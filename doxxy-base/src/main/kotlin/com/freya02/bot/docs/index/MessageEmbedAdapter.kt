package com.freya02.bot.docs.index

import com.google.gson.*
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.entities.EntityBuilder
import java.lang.reflect.Type

object MessageEmbedAdapter : JsonSerializer<MessageEmbed>, JsonDeserializer<MessageEmbed> {
    private val entityBuilder = EntityBuilder(null)
    private val GSON = Gson()

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MessageEmbed {
        val content = DataObject.fromJson(json.toString()).put("type", "rich")
        return entityBuilder.createMessageEmbed(content)
    }

    override fun serialize(src: MessageEmbed, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        //We need to serialize only the properties which JDA actually serializes for Discord
        val discordData = src.toData().toString()
        return GSON.fromJson(discordData, JsonElement::class.java)
    }
}