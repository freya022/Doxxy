package com.freya02.bot.docs.index;

import com.google.gson.*;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.EntityBuilder;

import java.lang.reflect.Type;

public class MessageEmbedAdapter implements JsonSerializer<MessageEmbed>, JsonDeserializer<MessageEmbed> {
	private static final Gson GSON = new GsonBuilder().create();

	private final EntityBuilder entityBuilder = new EntityBuilder(null);

	@Override
	public MessageEmbed deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		final DataObject content = DataObject.fromJson(json.toString());
		content.put("type", "rich");

		return entityBuilder.createMessageEmbed(content);
	}

	@Override
	public JsonElement serialize(MessageEmbed src, Type typeOfSrc, JsonSerializationContext context) {
		//We need to serialize only the properties which JDA actually serializes for Discord
		final String discordData = src.toData().toString();

		final Object dataTree = GSON.fromJson(discordData, Object.class);

		return GSON.toJsonTree(dataTree);
	}
}
