package com.freya02.docs.data;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.docs.DocParseException;
import com.freya02.docs.DocSourceType;
import com.freya02.docs.DocUtils;
import com.freya02.docs.DocsSession;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClassDoc extends BaseDoc {
	private final String sourceURL;
	private final DocSourceType source;

	@NotNull private final HTMLElement docTitleElement;
	@Nullable private final HTMLElement descriptionElement;
	@Nullable private final HTMLElement deprecationElement;
	@NotNull private final String className;

	@NotNull private final DetailToElementsMap detailToElementsMap;
	@Nullable private final SeeAlso seeAlso;

	@NotNull private final Map<String, FieldDoc> fieldDocs = new HashMap<>();
	@NotNull private final Map<String, MethodDoc> methodDocs = new HashMap<>();

	public ClassDoc(@NotNull DocsSession docsSession, @NotNull String url) throws IOException {
		this(docsSession, url, HttpUtils.getDocument(url));
	}

	public ClassDoc(@NotNull DocsSession docsSession, @NotNull String url, @NotNull Document document) throws IOException {
		this.sourceURL = url;
		this.source = DocSourceType.fromUrl(url);

		if (!DocUtils.isJavadocVersionCorrect(document)) {
			throw new IllegalArgumentException("Javadoc at '" + url + "' is not javadoc 17");
		}

		//Get javadoc title
		this.docTitleElement = HTMLElement.wrap(document.selectFirst("body > div.flex-box > div > main > div > h1"));

		//Get class name
		this.className = getClassName(url);

		//Get class description
		this.descriptionElement = HTMLElement.tryWrap(document.selectFirst("#class-description > div.block"));

		//Get class possible's deprecation
		this.deprecationElement = HTMLElement.tryWrap(document.selectFirst("#class-description > div.deprecation-block"));

		//Get class type parameters if they exist
		final Element detailTarget = document.selectFirst("#class-description");
		if (detailTarget == null) throw new DocParseException();

		this.detailToElementsMap = DetailToElementsMap.parseDetails(detailTarget);

		final DocDetail seeAlsoDetail = detailToElementsMap.getDetail(DocDetailType.SEE_ALSO);
		if (seeAlsoDetail != null) {
			this.seeAlso = new SeeAlso(source, seeAlsoDetail);
		} else {
			this.seeAlso = null;
		}

		processInheritedElements(docsSession, document, InheritedType.FIELD, this::onInheritedField);

		processInheritedElements(docsSession, document, InheritedType.METHOD, this::onInheritedMethod);

		//Try to find field details
		processDetailElements(document, ClassDetailType.FIELD, fieldElement -> {
			final FieldDoc fieldDocs = new FieldDoc(this, fieldElement);

			this.fieldDocs.put(fieldDocs.getElementId(), fieldDocs);
		});

		//Try to find enum constants, they're similar to fields it seems
		processDetailElements(document, ClassDetailType.ENUM_CONSTANTS, fieldElement -> {
			final FieldDoc fieldDocs = new FieldDoc(this, fieldElement);

			this.fieldDocs.put(fieldDocs.getElementId(), fieldDocs);
		});

		//Try to find method details
		processDetailElements(document, ClassDetailType.METHOD, methodElement -> {
			final MethodDoc methodDocs = new MethodDoc(this, methodElement);

			this.methodDocs.put(methodDocs.getElementId(), methodDocs);
		});

		//Try to find annotation "methods" (elements)
		processDetailElements(document, ClassDetailType.ANNOTATION_ELEMENT, annotationElement -> {
			final MethodDoc methodDocs = new MethodDoc(this, annotationElement);

			this.methodDocs.put(methodDocs.getElementId(), methodDocs);
		});
	}

	@NotNull
	private String getClassName(@NotNull String url) {
		final List<String> segments = HttpUrl.get(url).pathSegments();
		final String lastFragment = segments.get(segments.size() - 1);

		return lastFragment.substring(0, lastFragment.length() - 5); //Remove .html
	}

	private void processInheritedElements(@NotNull DocsSession docsSession, @NotNull Document document, @NotNull InheritedType inheritedType, @NotNull  BiConsumer<ClassDoc, String> inheritedElementConsumer) throws IOException {
		final Elements inheritedBlocks = document.select("section." + inheritedType.getClassSuffix() + "-summary > div.inherited-list");

		for (Element inheritedBlock : inheritedBlocks) {
			final Element title = inheritedBlock.selectFirst("h3");
			if (title == null) throw new DocParseException();

			final Element superClassLinkElement = title.selectFirst("a");
			if (superClassLinkElement != null) {
				final String superClassLink = superClassLinkElement.absUrl("href");

				final ClassDoc superClassDocs = docsSession.retrieveDoc(superClassLink);
				if (superClassDocs == null) continue; //Probably a bad link or an unsupported javadoc version

				for (Element element : inheritedBlock.select("code > a")) {
					final HttpUrl hrefUrl = HttpUrl.get(element.absUrl("href"));

					String targetId = hrefUrl.fragment();

					inheritedElementConsumer.accept(superClassDocs, targetId);
				}
			}
		}
	}

	private void onInheritedMethod(ClassDoc superClassDocs, String targetId) {
		//You can inherit a same method multiple times, it will show up multiple times in the docs
		// As the html is ordered such as the latest overridden method is shown, we can set the already existing doc to the newest one
		// Example: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/AbstractCollection.html#method.summary
		final MethodDoc methodDoc = superClassDocs.methodDocs.get(targetId);
		methodDocs.put(methodDoc.getElementId(), methodDoc);
	}

	private void onInheritedField(ClassDoc superClassDocs, String targetId) {
		final FieldDoc fieldDoc = superClassDocs.fieldDocs.get(targetId);

		fieldDocs.put(fieldDoc.getElementId(), fieldDoc);
	}

	public DocSourceType getSource() {
		return source;
	}

	@NotNull
	public HTMLElement getDocTitleElement() {
		return docTitleElement;
	}

	@NotNull
	public String getClassName() {
		return className;
	}

	@NotNull
	public Map<String, FieldDoc> getFieldDocs() {
		return fieldDocs;
	}

	@NotNull
	public Map<String, MethodDoc> getMethodDocs() {
		return methodDocs;
	}

	private void processDetailElements(@NotNull Document document, @NotNull ClassDetailType detailType, Consumer<@NotNull Element> callback) {
		final String detailId = detailType.getDetailId();

		//Get main blocks to determine what details are available (field, constructor (constr), method)
		final Element detailsSection = document.getElementById(detailId);
		if (detailsSection == null) return;

		for (Element element : detailsSection.select("ul.member-list > li > section.detail")) {
			callback.accept(element);
		}
	}

	@Override
	public String toString() {
		return "%s : %d fields, %d methods%s".formatted(className, fieldDocs.size(), methodDocs.size(), descriptionElement == null ? "" : " : " + descriptionElement.getTargetElement().text());
	}

	@Override
	@NotNull
	public String getEffectiveURL() {
		return source.toOnlineURL(sourceURL);
	}

	@Override
	@Nullable
	public HTMLElement getDescriptionElement() {
		return descriptionElement;
	}

	@Override
	@Nullable
	public HTMLElement getDeprecationElement() {
		return deprecationElement;
	}

	@Override
	@NotNull
	public DetailToElementsMap getDetailToElementsMap() {
		return detailToElementsMap;
	}

	@Nullable
	public SeeAlso getSeeAlso() {
		return seeAlso;
	}
}
