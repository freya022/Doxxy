package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.docs.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public class DocEmbeds {
	public static EmbedBuilder toEmbed(ClassDoc doc) {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(doc.getDocTitleElement().getTargetElement().text(), doc.getURL());

		fillDescription(doc, builder);

		fillDetails(builder, doc, EnumSet.noneOf(DocDetailType.class));

		addSeeAlso(builder, doc.getSeeAlso(), doc.getURL());

		return builder;
	}

	public static EmbedBuilder toEmbed(ClassDoc classDoc, MethodDoc methodDoc) {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(classDoc.getClassName() + '#' + methodDoc.getSimpleSignature() + " : " + methodDoc.getMethodReturnType(), methodDoc.getURL());

		//Should use that but JB annotations are duplicated, bruh momentum
//		builder.setTitle(methodDoc.getMethodSignature(), methodDoc.getURL());

		if (classDoc != methodDoc.getClassDocs()) {
			builder.setDescription("**Inherited from " + methodDoc.getClassDocs().getClassName() + "**\n\n");
		}

		fillDescription(methodDoc, builder);

		fillDetails(builder, methodDoc, EnumSet.noneOf(DocDetailType.class));

		addSeeAlso(builder, methodDoc.getSeeAlso(), methodDoc.getURL());

		return builder;
	}

	public static EmbedBuilder toEmbed(ClassDoc classDoc, FieldDoc fieldDoc) {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(classDoc.getClassName() + " : " + fieldDoc.getSimpleSignature(), fieldDoc.getURL());

		if (classDoc != fieldDoc.getClassDocs()) {
			builder.setDescription("**Inherited from " + fieldDoc.getClassDocs().getClassName() + "**\n\n");
		}

		fillDescription(fieldDoc, builder);

		fillDetails(builder, fieldDoc, EnumSet.noneOf(DocDetailType.class));

		addSeeAlso(builder, fieldDoc.getSeeAlso(), fieldDoc.getURL());

		return builder;
	}

	private static void fillDescription(BaseDoc doc, EmbedBuilder builder) {
		final HTMLElement descriptionElement = doc.getDescriptionElement();

		if (descriptionElement != null) {
			final String description = descriptionElement.getMarkdown();

			builder.appendDescription(
					getDescriptionValue(builder.getDescriptionBuilder().length(),
							description,
							doc.getURL())
			);
		} else {
			builder.appendDescription("No description");
		}
	}

	private static void fillDetails(EmbedBuilder builder, BaseDoc doc, EnumSet<DocDetailType> excludedTypes) {
		final List<DocDetail> details = doc.getDetails(excludedTypes);

		for (DocDetail detail : details) {
			addField(builder,
					detail.getDetailString(),
					detail.toMarkdown(),
					false,
					doc.getURL());
		}
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
