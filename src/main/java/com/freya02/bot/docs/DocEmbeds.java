package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.docs.DocUtils;
import com.freya02.docs.data.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class DocEmbeds {
	private static final int DESCRIPTION_MAX_LENGTH = 2048; //Description max length / 2 basically, otherwise embeds are HUGE

	@NotNull
	private static EnumSet<DocDetailType> getIncludedTypes() {
		return EnumSet.of( //SEE_ALSO not needed, already added manually
				DocDetailType.PARAMETERS,
				DocDetailType.TYPE_PARAMETERS,
				DocDetailType.RETURNS,
				DocDetailType.SPECIFIED_BY,
				DocDetailType.OVERRIDES,
				DocDetailType.INCUBATING,
				DocDetailType.DEFAULT,
				DocDetailType.THROWS
		);
	}

	public static EmbedBuilder toEmbed(ClassDoc doc) {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(doc.getDocTitleElement().getTargetElement().text(), getDocURL(doc));

		fillDescription(doc, builder);

		fillDeprecation(doc, builder);

		final List<FieldDoc> enumConstants = doc.getEnumConstants();
		if (!enumConstants.isEmpty()) {
			final StringJoiner valuesBuilder = new StringJoiner("`\n`", "`", "`");

			for (int i = 0, enumConstantsSize = enumConstants.size(); i < Math.min(10, enumConstantsSize); i++) { //Limit to 10
				FieldDoc enumConstant = enumConstants.get(i);

				valuesBuilder.add(enumConstant.getFieldName());
			}

			addField(builder, "Enum values:", valuesBuilder + (enumConstants.size() > 10 ? "\n... and more ..."  : ""), false, getDocURL(doc));
		}

		final List<MethodDoc> annotationElements = doc.getAnnotationElements();
		if (!annotationElements.isEmpty()) {
			final StringJoiner fieldsBuilder = new StringJoiner("`\n`", "`", "`");

			for (int i = 0, annotationElementsSize = annotationElements.size(); i < Math.min(10, annotationElementsSize); i++) { //Limit to 10
				MethodDoc annotationElement = annotationElements.get(i);

				fieldsBuilder.add("#" + annotationElement.getMethodName() + "()");
			}

			addField(builder, "Annotation fields:", fieldsBuilder + (annotationElements.size() > 10 ? "\n... and more ..."  : ""), false, getDocURL(doc));
		}

		fillDetails(builder, doc, getIncludedTypes());

		addSeeAlso(builder, doc.getSeeAlso(), getDocURL(doc));

		return builder;
	}

	@Nullable
	private static String getDocURL(BaseDoc doc) {
		return HttpUtils.doesStartByLocalhost(doc.getEffectiveURL())
				? null
				: doc.getEffectiveURL();
	}

	public static EmbedBuilder toEmbed(ClassDoc classDoc, MethodDoc methodDoc) {
		final EmbedBuilder builder = new EmbedBuilder();

		final String fixedReturnType = DocUtils.fixReturnType(methodDoc);

		String title = methodDoc.getMethodAnnotations() + "\n" + methodDoc.getSimpleAnnotatedSignature() + " : " + fixedReturnType;
		if (title.length() > MessageEmbed.TITLE_MAX_LENGTH) {
			title = "%s#%s : %s - [full signature on online docs]".formatted(methodDoc.getClassDocs().getClassName(), methodDoc.getMethodName(), methodDoc.getMethodReturnType());
		}

		builder.setTitle(title, getDocURL(methodDoc));

		//Should use that but JB annotations are duplicated, bruh momentum
//		builder.setTitle(methodDoc.getMethodSignature(), methodDoc.getURL());

		if (classDoc != methodDoc.getClassDocs()) {
			builder.setDescription("**Inherited from " + methodDoc.getClassDocs().getClassName() + "**\n\n");
		}

		fillDescription(methodDoc, builder);

		fillDeprecation(methodDoc, builder);

		fillDetails(builder, methodDoc, getIncludedTypes());

		addSeeAlso(builder, methodDoc.getSeeAlso(), getDocURL(methodDoc));

		return builder;
	}

	public static EmbedBuilder toEmbed(ClassDoc classDoc, FieldDoc fieldDoc) {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(classDoc.getClassName() + " : " + fieldDoc.getSimpleSignature(), getDocURL(fieldDoc));

		if (classDoc != fieldDoc.getClassDocs()) {
			builder.setDescription("**Inherited from " + fieldDoc.getClassDocs().getClassName() + "**\n\n");
		}

		fillDescription(fieldDoc, builder);

		fillDeprecation(fieldDoc, builder);

		fillDetails(builder, fieldDoc, getIncludedTypes());

		addSeeAlso(builder, fieldDoc.getSeeAlso(), getDocURL(fieldDoc));

		return builder;
	}

	private static void fillDescription(BaseDoc doc, EmbedBuilder builder) {
		final HTMLElement descriptionElement = doc.getDescriptionElement();

		if (descriptionElement != null) {
			final String description = descriptionElement.getMarkdown();

			builder.appendDescription(
					getDescriptionValue(builder.getDescriptionBuilder().length(),
							description,
							getDocURL(doc))
			);
		} else {
			builder.appendDescription("No description");
		}
	}

	private static void fillDeprecation(BaseDoc doc, EmbedBuilder builder) {
		final HTMLElement deprecationElement = doc.getDeprecationElement();

		if (deprecationElement != null) {
			addField(builder,
					"Deprecated",
					deprecationElement.getMarkdown(),
					false,
					getDocURL(doc));
		}
	}

	private static void fillDetails(EmbedBuilder builder, BaseDoc doc, EnumSet<DocDetailType> excludedTypes) {
		final List<DocDetail> details = doc.getDetails(excludedTypes);

		for (DocDetail detail : details) {
			addField(builder,
					detail.getDetailString(),
					detail.toMarkdown(),
					false,
					getDocURL(doc));
		}
	}

	private static String getDescriptionValue(int currentLength, @NotNull String descriptionValue, @Nullable String onlineTarget) {
		if (descriptionValue.length() + currentLength > DESCRIPTION_MAX_LENGTH) {
			if (onlineTarget == null) {
				return "Description is too long. Please look at the docs in your IDE";
			} else {
				return "Description is too long. Please look at [the online docs](" + onlineTarget + ")";
			}
		} else {
			return descriptionValue;
		}
	}

	private static void addField(EmbedBuilder builder, String fieldName, @NotNull String fieldValue, boolean inline, @Nullable String onlineDocs) {
		if (fieldValue.length() > MessageEmbed.VALUE_MAX_LENGTH) {
			if (onlineDocs == null) {
				builder.addField(fieldName, "This section is too long" + ". Please look at the docs in your IDE", inline);
			} else {
				builder.addField(fieldName, "This section is too long" + ". Please look at [the online docs](" + onlineDocs + ")", inline);
			}
		} else {
			builder.addField(fieldName, fieldValue, inline);
		}
	}

	private static void addSeeAlso(EmbedBuilder builder, SeeAlso seeAlso, String onlineDocs) {
		if (seeAlso != null) {
			final String seeAlsoMd = seeAlso.getReferences()
					.stream()
					.map(ref -> {
						if (HttpUtils.doesStartByLocalhost(ref.link())) {
							return ref.text();
						}

						return "[" + ref.text() + "](" + ref.link() + ")";
					})
					.collect(Collectors.joining(", "));

			addField(builder,
					"See Also",
					seeAlsoMd,
					false,
					onlineDocs);
		}
	}

}
