package com.freya02.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.bot.utils.Utils;
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

public class ClassDoc {
	private final String URL;

	@Nullable private final HTMLElement descriptionElement;
	@NotNull private final String className;
	@Nullable private final List<HTMLElement> typeParameters;

	@NotNull private final Map<String, FieldDoc> fieldDocs = new HashMap<>();
	@NotNull private final Map<String, MethodDoc> methodDocs = new HashMap<>();
	@Nullable private final Map<DocDetailType, List<HTMLElement>> detailToElementsMap;

	ClassDoc(@NotNull String url) throws IOException {
		this.URL = url;

		final Document document = Utils.getDocument(url);

		//Get class name
		final List<String> segments = HttpUrl.get(url).pathSegments();
		this.className = segments.get(segments.size() - 1).substring(0, segments.get(segments.size() - 1).length() - 5);

		//Get class description
		final Element descriptionElement = document.selectFirst("body > div.flex-box > div > main > section.description > div.block");
		if (descriptionElement != null) {
			this.descriptionElement = new HTMLElement(descriptionElement);
		} else {
			this.descriptionElement = null;
		}

		//Get class type parameters if they exist
		final Element detailListElement = document.selectFirst("body > div.flex-box > div > main > section.description > div.block + dl");
		if (detailListElement != null) {
			detailToElementsMap = MethodDoc.getDetailToElementsMap(detailListElement);
			this.typeParameters = detailToElementsMap.get(DocDetailType.TYPE_PARAMETERS);
		} else {
			detailToElementsMap = null;
			this.typeParameters = null;
		}

		processInheritedElements(document, InheritedType.FIELD, this::onInheritedField);

		processInheritedElements(document, InheritedType.METHOD, this::onInheritedMethod);

		//Try to find field details
		processDetailElements(document, ClassDetailType.FIELD, fieldElement -> {
			final FieldDoc fieldDocs = new FieldDoc(this, fieldElement);

			this.fieldDocs.put(fieldDocs.getElementId(), fieldDocs);
		});

		//Try to find method details
		processDetailElements(document, ClassDetailType.METHOD, methodElement -> {
			final MethodDoc methodDocs = new MethodDoc(this, methodElement);

			this.methodDocs.put(methodDocs.getElementId(), methodDocs);
		});
	}

	private void processInheritedElements(Document document, InheritedType inheritedType, BiConsumer<ClassDoc, String> inheritedElementConsumer) throws IOException {
		final Elements inheritedBlocks = document.select("section." + inheritedType + "-summary > div.inherited-list");

		for (Element inheritedBlock : inheritedBlocks) {
			final Element title = inheritedBlock.selectFirst("h3");
			if (title == null) throw new IllegalArgumentException();

			final Element superClassLink = title.selectFirst("a");
			if (superClassLink != null) {
				final ClassDoc superClassDocs = ClassDocs.of(superClassLink.absUrl("href"));

				for (Element element : inheritedBlock.select("code > a")) {
					final HttpUrl hrefUrl = HttpUrl.get(element.absUrl("href"));

					String targetId = hrefUrl.fragment();

					inheritedElementConsumer.accept(superClassDocs, targetId);
				}
			} else {
				final String[] titleSplit = title.text().split("\\s");
				final String fullName = titleSplit[titleSplit.length - 1]; //TODO should I still get the full qualification, and parse the package names ? in case the lib has classes the JDK has lol

				String className = Utils.getClassName(fullName);

				System.err.println(className);

				//TODO try to get from global cache
			}
		}
	}

	private void onInheritedMethod(ClassDoc superClassDocs, String targetId) {
		//You can inherit a same method multiple times, it will show up multiple times in the docs
		// As the html is ordered such as the latest overridden method is shown, we can set the already existing doc to the newest one
		// Example: https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/AbstractCollection.html#method.summary
		final MethodDoc methodDoc = superClassDocs.methodDocs.get(targetId);
		methodDocs.put(methodDoc.getElementId(), methodDoc);
	}

	private void onInheritedField(ClassDoc superClassDocs, String targetId) {
		final FieldDoc fieldDoc = superClassDocs.fieldDocs.get(targetId);

		fieldDocs.put(fieldDoc.getElementId(), fieldDoc);
	}

	@Nullable
	public HTMLElement getDescriptionElement() {
		return descriptionElement;
	}

	@NotNull
	public String getClassName() {
		return className;
	}

	public Map<DocDetailType, List<HTMLElement>> getDetailToElementsMap() {
		return detailToElementsMap;
	}

	public List<HTMLElement> getTypeParameters() {
		return typeParameters;
	}

	public Map<String, FieldDoc> getFieldDocs() {
		return fieldDocs;
	}

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

	public String getURL() {
		return URL;
	}
}
