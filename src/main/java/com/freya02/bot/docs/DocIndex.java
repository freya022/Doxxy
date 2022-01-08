package com.freya02.bot.docs;

import com.freya02.botcommands.api.Logging;
import com.freya02.docs.ClassDoc;
import com.freya02.docs.ClassDocs;
import com.freya02.docs.DocSourceType;
import com.freya02.docs.MethodDoc;
import com.google.gson.*;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static com.freya02.bot.Main.RENDERED_DOCS_CACHE_PATH;

public class DocIndex {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(MessageEmbed.class, new MessageEmbedAdapter())
			.create();

	private final ClassDocs classDocs;

	private final SimpleNameMap<CachedClass> simpleNameToCachedClassMap = new SimpleNameMap<>();

	//TODO Prepare some mechanism to not preload embeds of java classes (?)
	// Also filter based on java.* packages (yes)
	public DocIndex(DocSourceType sourceType) throws IOException {
		LOGGER.info("Loading docs for {}", sourceType.name());

		final Path sourceCacheFolder = RENDERED_DOCS_CACHE_PATH.resolve(sourceType.name());
		Files.createDirectories(sourceCacheFolder);

		this.classDocs = ClassDocs.indexAll(sourceType);

		for (String className : classDocs.getSimpleNameToUrlMap().keySet()) {
			final Path classCacheFolder = sourceCacheFolder.resolve(className);
			Files.createDirectories(classCacheFolder);

			final Path classCacheFile = classCacheFolder.resolve("Class.json");

			final boolean forceDownload = Files.notExists(classCacheFile);
			final ClassDoc doc = classDocs.tryRetrieveDoc(className, forceDownload);

			final CachedClass cachedClass;

			if (doc == null) {
				//cached, read the files

				final String json = Files.readString(classCacheFile);
				cachedClass = GSON.fromJson(json, CachedClass.class);
			} else {
				cachedClass = new CachedClass(getClassEmbed(doc));

				for (MethodDoc methodDoc : doc.getMethodDocs().values()) {
					final MessageEmbed methodEmbed = getMethodEmbed(doc, methodDoc);

					cachedClass.getMethodSignatureToJsonMap().put(methodDoc.getSimpleSignature(), methodEmbed);
				}

				Files.writeString(classCacheFile, GSON.toJson(cachedClass), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}

			simpleNameToCachedClassMap.put(className, cachedClass);
		}

		LOGGER.info("Docs loaded for {}", sourceType.name());
	}

	private MessageEmbed getClassEmbed(ClassDoc doc) {
		return DocEmbeds.toEmbed(doc).build();
	}

	private MessageEmbed getMethodEmbed(ClassDoc doc, MethodDoc methodDoc) {
		return DocEmbeds.toEmbed(doc, methodDoc).build();
	}

	/**
	 * Returns a JSON string of this method doc
	 */
	@Nullable
	public MessageEmbed getMethodDoc(String className, String methodId) {
		final CachedClass cachedClass = simpleNameToCachedClassMap.get(className);
		if (cachedClass == null) return null;

		return cachedClass.getMethodSignatureToJsonMap().get(methodId);
	}

	/**
	 * Returns a JSON string of this class doc
	 */
	@Nullable
	public MessageEmbed getClassDoc(String className) {
		final CachedClass cachedClass = simpleNameToCachedClassMap.get(className);
		if (cachedClass == null) return null;

		return cachedClass.getClassEmbed();
	}

	@Nullable
	public Collection<String> getMethodDocSuggestions(String className) {
		final CachedClass cachedClass = simpleNameToCachedClassMap.get(className);
		if (cachedClass == null) return Collections.emptyList();

		return cachedClass.getMethodSignatureToJsonMap().keySet();
	}

	@NotNull
	public Collection<String> getSimpleNameList() {
		return classDocs.getSimpleNameToUrlMap().keySet();
	}

	public static class SimpleNameMap<V> extends HashMap<String, V> {}

	//lmao, decided to keep the MessageEmbed in memory instead of the JSON strings,
	// don't really want to reconstruct them EVERYTIME its needed so why not, maybe
	private static class MessageEmbedAdapter implements JsonSerializer<MessageEmbed>, JsonDeserializer<MessageEmbed> {
		private final EntityBuilder entityBuilder = new EntityBuilder(null);

		@Override
		public MessageEmbed deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			final DataObject content = DataObject.fromJson(json.toString());
			content.put("type", "rich");

			return entityBuilder.createMessageEmbed(content);
		}

		@Override
		public JsonElement serialize(MessageEmbed src, Type typeOfSrc, JsonSerializationContext context) {
			//We need to serialize only the properties which JDA actually serializes for Discord
			final String discordData = src.toData().toString();

			final Object dataTree = GSON.fromJson(discordData, Object.class);

			return GSON.toJsonTree(dataTree);
		}
	}
}
