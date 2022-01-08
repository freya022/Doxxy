package com.freya02.bot.docs;

import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.HashMap;
import java.util.Map;

public class CachedClass {
	private final MessageEmbed classEmbed;
	private final Map<String, MessageEmbed> methodSignatureToJsonMap = new HashMap<>();

	public CachedClass(MessageEmbed classEmbed) {
		this.classEmbed = classEmbed;
	}

	public MessageEmbed getClassEmbed() {
		return classEmbed;
	}

	public Map<String, MessageEmbed> getMethodSignatureToJsonMap() {
		return methodSignatureToJsonMap;
	}
}
