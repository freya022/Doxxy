package com.freya02.bot.docs.cached;

import com.freya02.docs.SeeAlso;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public final class CachedMethod implements CachedDoc {
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

	@Override
	public List<SeeAlso.SeeAlsoReference> getSeeAlsoReferences() {
		return seeAlsoReferences;
	}
}