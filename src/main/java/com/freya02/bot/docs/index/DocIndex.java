package com.freya02.bot.docs.index;

import com.freya02.bot.docs.cached.CachedClass;
import com.freya02.bot.docs.cached.CachedClassMetadata;
import com.freya02.bot.docs.cached.CachedField;
import com.freya02.bot.docs.cached.CachedMethod;
import com.freya02.bot.utils.FileCache;
import com.freya02.botcommands.api.Logging;
import com.freya02.docs.ClassDocs;
import com.freya02.docs.DocSourceType;
import com.freya02.docs2.PageCache;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import static com.freya02.bot.Main.BOT_FOLDER;

public class DocIndex {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(MessageEmbed.class, new MessageEmbedAdapter())
			.create();

	private final IndexPaths indexPaths;
	private final DocSourceType sourceType;

	private final Path sourceCacheFolder;
	private DocIndexCache indexCache;

	private final FileCache renderedDocsCache;

	public DocIndex(DocSourceType sourceType) throws IOException {
		this.sourceType = sourceType;

		this.renderedDocsCache = new FileCache(BOT_FOLDER, "rendered_docs_" + sourceType.name(), true);

		this.sourceCacheFolder = renderedDocsCache.getCachePath().resolve(sourceType.name());
		Files.createDirectories(sourceCacheFolder);

		this.indexPaths = new IndexPaths(sourceCacheFolder);

		//Create an empty index as to not waste resources
		// As the real reindex is called when the versions checker is going to run
		this.indexCache = DocIndexCache.emptyIndex(indexPaths);
	}

	//TODO make so DocIndexCache reads local data on initial run and then reindex if version changed
	//Must be called on startup
	// Otherwise the index cache will be empty until the next update
	public DocIndex reindex() throws IOException {
		LOGGER.info("Re-indexing docs for {}", sourceType.name());

		LOGGER.info("Clearing cache for {}", sourceType.name());
		PageCache.INSTANCE.clearCache(sourceType);
		LOGGER.info("Cleared cache of {}", sourceType.name());

		this.indexCache = DocIndexCache.indexDocs(ClassDocs.getUpdatedSource(sourceType), this.sourceCacheFolder, indexPaths);

		LOGGER.info("Re-indexed docs for {}", sourceType.name());

		System.gc(); //Very effective

		return this;
	}

	@Nullable
	public CachedMethod getMethodDoc(String className, String methodId) throws IOException {
		final CachedClassMetadata cachedClass = indexCache.getMethodHolderSimpleNames().get(className);
		if (cachedClass == null) return null;

		final Path methodEmbedPath = indexPaths.getMethodEmbedPath(className, methodId);
		if (Files.notExists(methodEmbedPath)) return null;

		return GSON.fromJson(Files.readString(methodEmbedPath), CachedMethod.class);
	}

	@Nullable
	public CachedMethod getMethodDoc(String fullSignature) throws IOException {
		final String[] split = fullSignature.split("#");
		if (split.length != 2) return null;

		return getMethodDoc(split[0], split[1]);
	}

	@Nullable
	public CachedField getFieldDoc(String className, String fieldName) throws IOException {
		final CachedClassMetadata cachedClass = indexCache.getFieldHolderSimpleNames().get(className);
		if (cachedClass == null) return null;

		final Path fieldEmbedPath = indexPaths.getFieldEmbedPath(className, fieldName);
		if (Files.notExists(fieldEmbedPath)) return null;

		return GSON.fromJson(Files.readString(fieldEmbedPath), CachedField.class);
	}

	public CachedField getFieldDoc(String fullSignature) throws IOException {
		final String[] split = fullSignature.split("#");
		if (split.length != 2) return null;

		return getFieldDoc(split[0], split[1]);
	}

	@Nullable
	public CachedClass getClassDoc(String className) throws IOException {
		final CachedClassMetadata cachedClass = indexCache.getSimpleNameToCachedClassMap().get(className);
		if (cachedClass == null) return null;

		final Path classEmbedPath = indexPaths.getClassEmbedPath(className);
		if (Files.notExists(classEmbedPath)) return null;

		return GSON.fromJson(Files.readString(classEmbedPath), CachedClass.class);
	}

	@NotNull
	public Collection<String> getMethodDocSuggestions(String className) {
		final CachedClassMetadata cachedClass = indexCache.getMethodHolderSimpleNames().get(className);
		if (cachedClass == null) return Collections.emptyList();

		return cachedClass.getMethodSignatureToFileNameMap().keySet();
	}

	@NotNull
	public Collection<String> getMethodDocSuggestions() {
		return indexCache.getAllFullMethodSignatures();
	}

	@NotNull
	public Collection<String> getSimpleNameList() {
		return indexCache.getSimpleNameToCachedClassMap().keySet();
	}

	@NotNull
	public Collection<String> getClassesWithMethods() {
		return indexCache.getMethodHolderSimpleNames().keySet();
	}

	@NotNull
	public Collection<String> getClassesWithFields() {
		return indexCache.getFieldHolderSimpleNames().keySet();
	}

	@NotNull
	public Collection<String> getFieldDocSuggestions(String className) {
		final CachedClassMetadata cachedClass = indexCache.getFieldHolderSimpleNames().get(className);
		if (cachedClass == null) return Collections.emptyList();

		return cachedClass.getFieldNameToFileNameMap().keySet();
	}

	@NotNull
	public Collection<String> getFieldDocSuggestions() {
		return indexCache.getAllFullFieldSignatures();
	}

	public void close() throws IOException {
		renderedDocsCache.close();
	}

	public Collection<String> getMethodAndFieldDocSuggestions(String className) {
		final CachedClassMetadata cachedClass = indexCache.getSimpleNameToCachedClassMap().get(className);
		if (cachedClass == null) return Collections.emptyList();

		return Sets.union(cachedClass.getMethodSignatureToFileNameMap().keySet(), cachedClass.getFieldNameToFileNameMap().keySet());
	}
}
