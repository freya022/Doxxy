package com.freya02.bot.docs.cached;

import com.freya02.docs.SeeAlso;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public final class CachedClass implements CachedDoc {
	private final MessageEmbed classEmbed;
	private final List<SeeAlso.SeeAlsoReference> seeAlsoReferences;

	public CachedClass(MessageEmbed classEmbed,
	                   List<SeeAlso.SeeAlsoReference> seeAlsoReferences) {
		this.classEmbed = classEmbed;
		this.seeAlsoReferences = seeAlsoReferences;
	}

	public MessageEmbed getClassEmbed() {
		return classEmbed;
	}

	@Override
	public List<SeeAlso.SeeAlsoReference> getSeeAlsoReferences() {
		return seeAlsoReferences;
	}
}