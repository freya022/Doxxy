package com.freya02.bot.docs;

import com.freya02.bot.utils.CryptoUtils;
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

	private final SimpleNameMap<CachedClassMetadata> simpleNameToCachedClassMap = new SimpleNameMap<>();
	private final Path sourceCacheFolder;

	public DocIndex(DocSourceType sourceType) throws IOException {
		LOGGER.info("Loading docs for {}", sourceType.name());

		this.sourceCacheFolder = RENDERED_DOCS_CACHE_PATH.resolve(sourceType.name());
		Files.createDirectories(sourceCacheFolder);

		this.classDocs = ClassDocs.indexAll(sourceType);

		indexAll();

		LOGGER.info("Docs loaded for {}", sourceType.name());
	}

	@NotNull
	private Path getClassEmbedPath(String className) {
		return sourceCacheFolder.resolve(className).resolve("ClassEmbed.json");
	}

	@NotNull
	private Path getMethodEmbedPath(String className, String methodId) {
		return sourceCacheFolder.resolve(className).resolve(getMethodFileName(methodId));
	}

	public synchronized void indexAll() throws IOException {
		simpleNameToCachedClassMap.clear();

		for (String className : classDocs.getSimpleNameToUrlMap().keySet()) {
			final Path classCacheFolder = sourceCacheFolder.resolve(className);
			Files.createDirectories(classCacheFolder);

			final Path classMetadataCacheFile = classCacheFolder.resolve("ClassMetadata.json");
			final Path classEmbedCacheFile = getClassEmbedPath(className);

			final boolean forceDownload = Files.notExists(classMetadataCacheFile) || Files.notExists(classEmbedCacheFile);
			final ClassDoc doc = classDocs.tryRetrieveDoc(className, forceDownload);

			final CachedClassMetadata cachedClassMetadata;

			if (doc == null) {
				//cached, read the files

				final String metadataJson = Files.readString(classMetadataCacheFile);
				cachedClassMetadata = GSON.fromJson(metadataJson, CachedClassMetadata.class);
			} else {
				cachedClassMetadata = new CachedClassMetadata();

				final MessageEmbed classEmbed = getClassEmbed(doc);

				Files.writeString(classEmbedCacheFile, GSON.toJson(classEmbed), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

				for (MethodDoc methodDoc : doc.getMethodDocs().values()) {
					final MessageEmbed methodEmbed = getMethodEmbed(doc, methodDoc);

					final String methodFileName = getMethodFileName(methodDoc.getSimpleSignature());
					cachedClassMetadata.getMethodSignatureToFileNameMap().put(
							methodDoc.getSimpleSignature(),
							methodFileName
					);

					Files.writeString(classCacheFolder.resolve(methodFileName), GSON.toJson(methodEmbed), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				}

				Files.writeString(classMetadataCacheFile, GSON.toJson(cachedClassMetadata), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}

			simpleNameToCachedClassMap.put(className, cachedClassMetadata);
		}
	}

	@NotNull
	private String getMethodFileName(@NotNull String signature) {
		return CryptoUtils.hash(signature) + ".json";
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
	public MessageEmbed getMethodDoc(String className, String methodId) throws IOException {
		final CachedClassMetadata cachedClass = simpleNameToCachedClassMap.get(className);
		if (cachedClass == null) return null;

		return GSON.fromJson(Files.readString(getMethodEmbedPath(className, methodId)), MessageEmbed.class);
	}

	/**
	 * Returns a JSON string of this class doc
	 */
	@Nullable
	public MessageEmbed getClassDoc(String className) throws IOException {
		final CachedClassMetadata cachedClass = simpleNameToCachedClassMap.get(className);
		if (cachedClass == null) return null;

		return GSON.fromJson(Files.readString(getClassEmbedPath(className)), MessageEmbed.class);
	}

	@Nullable
	public Collection<String> getMethodDocSuggestions(String className) {
		final CachedClassMetadata cachedClass = simpleNameToCachedClassMap.get(className);
		if (cachedClass == null) return Collections.emptyList();

		return cachedClass.getMethodSignatureToFileNameMap().keySet();
	}

	@NotNull
	public Collection<String> getSimpleNameList() {
		return simpleNameToCachedClassMap.keySet();
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
