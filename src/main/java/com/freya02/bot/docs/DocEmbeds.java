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

		addSeeAlso(builder, doc.getSeeAlso(), doc.getURL());

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

			builder.appendDescription(
					getDescriptionValue(builder.getDescriptionBuilder().length(),
							description,
							methodDoc.getURL())
			);
		} else {
			builder.appendDescription("No description");
		}

		final List<HTMLElement> parameterElements = methodDoc.getParameterElements();
		if (parameterElements != null) {
			addField(builder,
					"Parameters",
					parameterElements.stream().map(HTMLElement::getMarkdown3).collect(Collectors.joining("\n")),
					false,
					methodDoc.getURL());
		}

		final HTMLElement returnsElement = methodDoc.getReturnsElement();
		if (returnsElement != null) {
			addField(builder,"Returns", returnsElement.getMarkdown3(), false, methodDoc.getURL());
		}

		final List<HTMLElement> throwsElements = methodDoc.getThrowsElements();
		if (throwsElements != null) {
			addField(builder,
					"Throws",
					throwsElements.stream().map(HTMLElement::getMarkdown3).collect(Collectors.joining("\n")),
					false,
					methodDoc.getURL());
		}

		final HTMLElement defaultElement = methodDoc.getDefaultElement();
		if (defaultElement != null) {
			addField(builder,"Default", defaultElement.getMarkdown3(), false, methodDoc.getURL());
		}

		final HTMLElement incubatingElement = methodDoc.getIncubatingElement();
		if (incubatingElement != null) {
			addField(builder, "Incubating", incubatingElement.getMarkdown3(), false, methodDoc.getURL());
		}

		final List<HTMLElement> typeParameters = methodDoc.getTypeParameterElements();
		if (typeParameters != null) {
			addField(builder,
					"Type parameters",
					typeParameters.stream().map(HTMLElement::getMarkdown3).collect(Collectors.joining("\n")),
					false,
					methodDoc.getURL());
		}

		addSeeAlso(builder, methodDoc.getSeeAlso(), methodDoc.getURL());

		return builder;
	}

	private static String getDescriptionValue(int currentLength, String descriptionValue, String onlineTarget) {
		if (descriptionValue.length() + currentLength > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
			return "Description is too long. Please look at [the online docs](" + onlineTarget + ")";
		} else {
			return descriptionValue;
		}
	}

	private static void addField(EmbedBuilder builder, String fieldName, String fieldValue, boolean inline, String onlineDocs) {
		if (fieldValue.length() > MessageEmbed.VALUE_MAX_LENGTH) {
			builder.addField(fieldName, "This section is too long" + ". Please look at [the online docs](" + onlineDocs + ")", inline);
		} else {
			builder.addField(fieldName, fieldValue, inline);
		}
	}

	private static void addSeeAlso(EmbedBuilder builder, SeeAlso seeAlso, String onlineDocs) {
		if (seeAlso != null) {
			final String seeAlsoMd = seeAlso.getReferences().stream().map(ref -> "[" + ref.text() + "](" + ref.link() + ")").collect(Collectors.joining(", "));

			addField(builder,
					"See Also",
					seeAlsoMd,
					false,
					onlineDocs);
		}
	}
}
