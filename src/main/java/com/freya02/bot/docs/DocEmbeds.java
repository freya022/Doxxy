package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.bot.utils.IOUtils;
import com.freya02.docs.ClassDoc;
import com.freya02.docs.MethodDoc;
import com.freya02.docs.SeeAlso;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EntityBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

	public static EmbedBuilder retrieveEmbed(JDA jda, String url) throws IOException {
		final String downloadedBody = HttpUtils.downloadBodyIfNotCached(url);

		//TODO transform url into MD5
		// Actively get target method and target from the URL
		// Remove global cache in ClassDocs, OkHttp will cache the actual network resources, however this will waste a bit more CPU for non-cached *requested* classes
		// No worries for dependencies changing their docs - the dependencies doesn't change if the docs aren't updated
		final Path cachedJsonPath = IOUtils.changeExtension(HttpUtils.getCachePathForUrl(url), "json");
		if (downloadedBody == null && Files.exists(cachedJsonPath)) {
			final EntityBuilder entityBuilder = ((JDAImpl) jda).getEntityBuilder();
			final DataObject cachedJsonData = DataObject.fromJson(Files.readAllBytes(cachedJsonPath));
			cachedJsonData.put("type", "rich"); //see EmbedType

			return new EmbedBuilder(entityBuilder.createMessageEmbed(cachedJsonData));
		} else {
			//Get with network call

			return toEmbed(new ClassDoc(url, HttpUtils.parseDocument(downloadedBody, url)));
		}
	}
}
