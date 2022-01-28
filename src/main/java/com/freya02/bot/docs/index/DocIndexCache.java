package com.freya02.bot.docs.index;

import com.freya02.bot.docs.DocEmbeds;
import com.freya02.bot.docs.cached.CachedClass;
import com.freya02.bot.docs.cached.CachedClassMetadata;
import com.freya02.bot.docs.cached.CachedField;
import com.freya02.bot.docs.cached.CachedMethod;
import com.freya02.docs.ClassDocs;
import com.freya02.docs.DocsSession;
import com.freya02.docs.data.ClassDoc;
import com.freya02.docs.data.FieldDoc;
import com.freya02.docs.data.MethodDoc;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DocIndexCache {
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(MessageEmbed.class, new MessageEmbedAdapter())
			.create();

	private final SimpleNameMap<CachedClassMetadata> simpleNameToCachedClassMap = new SimpleNameMap<>();

	private final SimpleNameMap<CachedClassMetadata> methodHolderSimpleNames = new SimpleNameMap<>();
	private final SimpleNameMap<CachedClassMetadata> fieldHolderSimpleNames = new SimpleNameMap<>();

	private final List<String> allFullMethodSignatures = new ArrayList<>();
	private final List<String> allFullFieldSignatures = new ArrayList<>();
	private final IndexPaths indexPaths;

	private DocIndexCache(IndexPaths indexPaths) {
		this.indexPaths = indexPaths;
	}

	//Could replace the "force" argument
	// As to only update if the version has really changed
	private DocIndexCache(ClassDocs classDocs,
	                      Path sourceCacheFolder,
	                      IndexPaths indexPaths,
	                      boolean force) {
		this.indexPaths = indexPaths;

		final DocsSession docsSession = new DocsSession();

		for (Map.Entry<String, String> entry : classDocs.getSimpleNameToUrlMap().entrySet()) {
			final String className = entry.getKey();
			final String classUrl = entry.getValue();

			try {
				final Path classCacheFolder = sourceCacheFolder.resolve(className);
				Files.createDirectories(classCacheFolder);

				final Path classMetadataCacheFile = classCacheFolder.resolve("ClassMetadata.json");
				final Path classEmbedCacheFile = indexPaths.getClassEmbedPath(className);

				final boolean forceDownload = force || Files.notExists(classMetadataCacheFile) || Files.notExists(classEmbedCacheFile);
				final ClassDoc doc;
				if (forceDownload) doc = docsSession.retrieveDoc(classUrl);
				else doc = docsSession.retrieveDocIfNotCached(classUrl);

				final CachedClassMetadata cachedClassMetadata;

				if (doc == null) {
					//cached, read the files

					final String metadataJson = Files.readString(classMetadataCacheFile);
					cachedClassMetadata = GSON.fromJson(metadataJson, CachedClassMetadata.class);
				} else {
					cachedClassMetadata = new CachedClassMetadata();

					final MessageEmbed classEmbed = DocEmbeds.toEmbed(doc).build();

					final CachedClass cachedClass = new CachedClass(classEmbed, doc.getSeeAlso() == null
							? null
							: doc.getSeeAlso().getReferences());

					Files.writeString(classEmbedCacheFile, GSON.toJson(cachedClass), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

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

				for (String methodSignature : cachedClassMetadata.getMethodSignatureToFileNameMap().keySet()) {
					allFullMethodSignatures.add(className + "#" + methodSignature);
				}

				for (String fieldName : cachedClassMetadata.getFieldNameToFileNameMap().keySet()) {
					allFullFieldSignatures.add(className + "#" + fieldName);
				}
			} catch (Exception e) {
				throw new RuntimeException("An exception occurred while reading the docs of " + className, e);
			}
		}
	}

	public static DocIndexCache indexDocs(ClassDocs classDocs,
	                                      Path sourceCacheFolder,
	                                      IndexPaths indexPaths,
	                                      boolean force) {
		return new DocIndexCache(classDocs, sourceCacheFolder, indexPaths, force);
	}

	public static DocIndexCache emptyIndex(IndexPaths indexPaths) {
		return new DocIndexCache(indexPaths);
	}

	private void addMethodDocs(String className, Path classCacheFolder, ClassDoc doc, CachedClassMetadata cachedClassMetadata) {
		for (MethodDoc methodDoc : doc.getMethodDocs().values()) {
			try {
				final MessageEmbed methodEmbed = DocEmbeds.toEmbed(doc, methodDoc).build();

				final String methodFileName = indexPaths.getMethodFileName(methodDoc.getSimpleSignature());
				cachedClassMetadata.getMethodSignatureToFileNameMap().put(
						methodDoc.getSimpleSignature(),
						methodFileName
				);

				final CachedMethod cachedMethod = new CachedMethod(methodEmbed, methodDoc.getSeeAlso() == null
						? null
						: methodDoc.getSeeAlso().getReferences());

				Files.writeString(classCacheFolder.resolve(methodFileName), GSON.toJson(cachedMethod), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

				final MessageEmbed fieldEmbed = DocEmbeds.toEmbed(doc, fieldDoc).build();

				final String fieldFileName = indexPaths.getFieldFileName(fieldDoc.getFieldName());
				cachedClassMetadata.getFieldNameToFileNameMap().put(
						fieldDoc.getFieldName(),
						fieldFileName
				);

				final CachedField cachedField = new CachedField(fieldEmbed, fieldDoc.getSeeAlso() == null
						? null
						: fieldDoc.getSeeAlso().getReferences());

				Files.writeString(classCacheFolder.resolve(fieldFileName), GSON.toJson(cachedField), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (Exception e) {
				throw new RuntimeException("An exception occurred while reading the docs of " + className + "#" + fieldDoc.getFieldName(), e);
			}
		}
	}

	public SimpleNameMap<CachedClassMetadata> getSimpleNameToCachedClassMap() {
		return simpleNameToCachedClassMap;
	}

	public SimpleNameMap<CachedClassMetadata> getMethodHolderSimpleNames() {
		return methodHolderSimpleNames;
	}

	public SimpleNameMap<CachedClassMetadata> getFieldHolderSimpleNames() {
		return fieldHolderSimpleNames;
	}

	public List<String> getAllFullMethodSignatures() {
		return allFullMethodSignatures;
	}

	public List<String> getAllFullFieldSignatures() {
		return allFullFieldSignatures;
	}
}
