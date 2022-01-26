package com.freya02.bot.docs;

import com.freya02.docs.SeeAlso;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public final class CachedMethod {
	private final MessageEmbed methodEmbed;
	private final List<SeeAlso.SeeAlsoReference> seeAlsoReferences;

	public CachedMethod(MessageEmbed methodEmbed,
	                    List<SeeAlso.SeeAlsoReference> seeAlsoReferences) {
		this.methodEmbed = methodEmbed;
		this.seeAlsoReferences = seeAlsoReferences;
	}

	public MessageEmbed getMethodEmbed() {
		return methodEmbed;
	}

	public List<SeeAlso.SeeAlsoReference> getSeeAlsoReferences() {
		return seeAlsoReferences;
	}
}