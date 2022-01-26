package com.freya02.bot.docs;

import com.freya02.docs.SeeAlso;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;

public final class CachedField {
	private final MessageEmbed fieldEmbed;
	private final List<SeeAlso.SeeAlsoReference> seeAlsoReferences;

	public CachedField(MessageEmbed fieldEmbed,
	                   List<SeeAlso.SeeAlsoReference> seeAlsoReferences) {
		this.fieldEmbed = fieldEmbed;
		this.seeAlsoReferences = seeAlsoReferences;
	}

	public MessageEmbed getFieldEmbed() {
		return fieldEmbed;
	}

	public List<SeeAlso.SeeAlsoReference> getSeeAlsoReferences() {
		return seeAlsoReferences;
	}
}