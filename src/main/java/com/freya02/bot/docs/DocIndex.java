package com.freya02.bot.docs;

import com.freya02.bot.utils.CryptoUtils;
import com.freya02.botcommands.api.Logging;
import com.freya02.docs.*;
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
	private final SimpleNameMap<CachedClassMetadata> methodHolderSimpleNames = new SimpleNameMap<>();
	private final SimpleNameMap<CachedClassMetadata> fieldHolderSimpleNames = new SimpleNameMap<>();
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

	@NotNull
	private Path getFieldEmbedPath(String className, String fieldName) {
		return sourceCacheFolder.resolve(className).resolve(getFieldFileName(fieldName));
	}

	public synchronized void indexAll() {
		simpleNameToCachedClassMap.clear();
		methodHolderSimpleNames.clear();
		fieldHolderSimpleNames.clear();

		for (String className : classDocs.getSimpleNameToUrlMap().keySet()) {
			try {
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

					addMethodDocs(className, classCacheFolder, doc, cachedClassMetadata);

					addFieldDocs(className, classCacheFolder, doc, cachedClassMetadata);

					Files.writeString(classMetadataCacheFile, GSON.toJson(cachedClassMetadata), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				}

				simpleNameToCachedClassMap.put(className, cachedClassMetadata);

				if (!cachedClassMetadata.getMethodSignatureToFileNameMap().isEmpty()) {
					methodHolderSimpleNames.put(className, cachedClassMetadata);
				}

				if (!cachedClassMetadata.getFieldNameToFileNameMap().isEmpty()) {
					fieldHolderSimpleNames.put(className, cachedClassMetadata);
				}
			} catch (Exception e) {
				throw new RuntimeException("An exception occurred while reading the docs of " + className, e);
			}
		}
	}

	private void addMethodDocs(String className, Path classCacheFolder, ClassDoc doc, CachedClassMetadata cachedClassMetadata) {
		for (MethodDoc methodDoc : doc.getMethodDocs().values()) {
			try {
				final MessageEmbed methodEmbed = getMethodEmbed(doc, methodDoc);

				final String methodFileName = getMethodFileName(methodDoc.getSimpleSignature());
				cachedClassMetadata.getMethodSignatureToFileNameMap().put(
						methodDoc.getSimpleSignature(),
						methodFileName
				);

				Files.writeString(classCacheFolder.resolve(methodFileName), GSON.toJson(methodEmbed), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (Exception e) {
				throw new RuntimeException("An exception occurred while reading the docs of " + className + "#" + methodDoc.getSimpleSignature(), e);
			}
		}
	}

	private void addFieldDocs(String className, Path classCacheFolder, ClassDoc doc, CachedClassMetadata cachedClassMetadata) {
		for (FieldDoc fieldDoc : doc.getFieldDocs().values()) {
			try {
				//If not a public constant then skip
				if (!fieldDoc.getModifiers().equals("public static final")) {
					continue;
				}

				final MessageEmbed fieldEmbed = getFieldEmbed(doc, fieldDoc);

				final String fieldFileName = getFieldFileName(fieldDoc.getFieldName());
				cachedClassMetadata.getFieldNameToFileNameMap().put(
						fieldDoc.getFieldName(),
						fieldFileName
				);

				Files.writeString(classCacheFolder.resolve(fieldFileName), GSON.toJson(fieldEmbed), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (Exception e) {
				throw new RuntimeException("An exception occurred while reading the docs of " + className + "#" + fieldDoc.getFieldName(), e);
			}
		}
	}

	private MessageEmbed getFieldEmbed(ClassDoc doc, FieldDoc fieldDoc) {
		return DocEmbeds.toEmbed(doc, fieldDoc).build();
	}

	@NotNull
	private String getMethodFileName(@NotNull String signature) {
		return CryptoUtils.hash(signature) + ".json";
	}

	private String getFieldFileName(String fieldName) {
		return CryptoUtils.hash(fieldName) + ".json";
	}

	private MessageEmbed getClassEmbed(ClassDoc doc) {
		return DocEmbeds.toEmbed(doc).build();
	}

	private MessageEmbed getMethodEmbed(ClassDoc doc, MethodDoc methodDoc) {
		return DocEmbeds.toEmbed(doc, methodDoc).build();
	}

	@Nullable
	public MessageEmbed getMethodDoc(String className, String methodId) throws IOException {
		final CachedClassMetadata cachedClass = methodHolderSimpleNames.get(className);
		if (cachedClass == null) return null;

		final Path methodEmbedPath = getMethodEmbedPath(className, methodId);
		if (Files.notExists(methodEmbedPath)) return null;

		return GSON.fromJson(Files.readString(methodEmbedPath), MessageEmbed.class);
	}

	@Nullable
	public MessageEmbed getFieldDoc(String className, String fieldName) throws IOException {
		final CachedClassMetadata cachedClass = fieldHolderSimpleNames.get(className);
		if (cachedClass == null) return null;

		final Path fieldEmbedPath = getFieldEmbedPath(className, fieldName);
		if (Files.notExists(fieldEmbedPath)) return null;

		return GSON.fromJson(Files.readString(fieldEmbedPath), MessageEmbed.class);
	}

	@Nullable
	public MessageEmbed getClassDoc(String className) throws IOException {
		final CachedClassMetadata cachedClass = simpleNameToCachedClassMap.get(className);
		if (cachedClass == null) return null;

		final Path classEmbedPath = getClassEmbedPath(className);
		if (Files.notExists(classEmbedPath)) return null;

		return GSON.fromJson(Files.readString(classEmbedPath), MessageEmbed.class);
	}

	@Nullable
	public Collection<String> getMethodDocSuggestions(String className) {
		final CachedClassMetadata cachedClass = methodHolderSimpleNames.get(className);
		if (cachedClass == null) return Collections.emptyList();

		return cachedClass.getMethodSignatureToFileNameMap().keySet();
	}

	@NotNull
	public Collection<String> getSimpleNameList() {
		return simpleNameToCachedClassMap.keySet();
	}

	public Collection<String> getMethodHolderSimpleNames() {
		return fieldHolderSimpleNames.keySet();
	}

	public Collection<String> getFieldHolderSimpleNames() {
		return fieldHolderSimpleNames.keySet();
	}

	public Collection<String> getFieldDocSuggestions(String className) {
		final CachedClassMetadata cachedClass = fieldHolderSimpleNames.get(className);
		if (cachedClass == null) return Collections.emptyList();

		return cachedClass.getFieldNameToFileNameMap().keySet();
	}

	public static class SimpleNameMap<V> extends HashMap<String, V> {}

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
