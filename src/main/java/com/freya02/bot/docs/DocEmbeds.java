package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.docs.ClassDoc;
import com.freya02.docs.MethodDoc;
import com.freya02.docs.SeeAlso;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.List;
import java.util.stream.Collectors;

public class DocEmbeds {
	public static EmbedBuilder toEmbed(ClassDoc doc) {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(doc.getDocTitleElement().getTargetElement().text(), doc.getURL());

		final HTMLElement descriptionElement = doc.getDescriptionElement();
		if (descriptionElement != null) {
			final String description = descriptionElement.getMarkdown3();
			if (description.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
				builder.setDescription("Description is too long, please look at the [docs page](" + doc.getURL() + ")");
			} else {
				builder.setDescription(description);
			}
		} else {
			builder.setDescription("No description");
		}

		final List<HTMLElement> typeParameters = doc.getTypeParameterElements();
		if (typeParameters != null) {
			builder.addField("Type parameters", typeParameters.stream().map(HTMLElement::getMarkdown3).collect(Collectors.joining("\n")), false);
		}

		addSeeAlso(builder, doc.getSeeAlso());

		return builder;
	}

	public static EmbedBuilder toEmbed(ClassDoc classDoc, MethodDoc methodDoc) {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(classDoc.getClassName() + '#' + methodDoc.getSimpleSignature(), methodDoc.getURL());

		if (classDoc != methodDoc.getClassDocs()) {
			builder.setDescription("**Inherited from " + methodDoc.getClassDocs().getClassName() + "**\n\n");
		}

		final HTMLElement descriptionElement = methodDoc.getDescriptionElement();
		if (descriptionElement != null) {
			final String description = descriptionElement.getMarkdown3();
			if (description.length() + builder.getDescriptionBuilder().length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
				builder.appendDescription("Description is too long, please look at the [docs page](" + methodDoc.getURL() + ")");
			} else {
				builder.appendDescription(description);
			}
		} else {
			builder.appendDescription("No description");
		}

		final List<HTMLElement> parameterElements = methodDoc.getParameterElements();
		if (parameterElements != null) {
			builder.addField("Parameters", parameterElements.stream().map(HTMLElement::getMarkdown3).collect(Collectors.joining("\n")), false);
		}

		final HTMLElement returnsElement = methodDoc.getReturnsElement();
		if (returnsElement != null) {
			builder.addField("Returns", returnsElement.getMarkdown3(), false);
		}

		final HTMLElement defaultElement = methodDoc.getDefaultElement();
		if (defaultElement != null) {
			builder.addField("Default", defaultElement.getMarkdown3(), false);
		}

		final HTMLElement incubatingElement = methodDoc.getIncubatingElement();
		if (incubatingElement != null) {
			builder.addField("Incubating", incubatingElement.getMarkdown3(), false);
		}

		final List<HTMLElement> typeParameters = methodDoc.getTypeParameterElements();
		if (typeParameters != null) {
			builder.addField("Type parameters", typeParameters.stream().map(HTMLElement::getMarkdown3).collect(Collectors.joining("\n")), false);
		}

		addSeeAlso(builder, methodDoc.getSeeAlso());

		return builder;
	}

	private static void addSeeAlso(EmbedBuilder builder, SeeAlso seeAlso) {
		if (seeAlso != null) {
			final String seeAlsoMd = seeAlso.getReferences().stream().map(ref -> "[" + ref.text() + "](" + ref.link() + ")").collect(Collectors.joining(", "));

			if (seeAlsoMd.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
				builder.addField("See Also", seeAlsoMd, false);
			}
		}
	}
}
