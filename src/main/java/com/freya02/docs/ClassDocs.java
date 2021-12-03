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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClassDocs {
	@Nullable private final HTMLElement descriptionElement;
	@NotNull private final String className;
	@Nullable private final List<HTMLElement> typeParameters;

	private final List<FieldDocs> fieldDocs = new ArrayList<>();
	private final Map<String, MethodDoc> methodDocs = new HashMap<>();

	public ClassDocs(@NotNull String url) throws IOException {
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
			final Map<DocDetailType, List<HTMLElement>> map = MethodDoc.getDetailToElementsMap(detailListElement);
			this.typeParameters = map.get(DocDetailType.TYPE_PARAMETERS);
		} else {
			this.typeParameters = null;
		}

		//TODO add global class cache
		// in order to add super methods
		// Method overridden by classes are NOT present in the "Method inherited from X" list
		// Extract overridden method name with text()
		// Associate back with the h3 text (is the class)
		// Have to parse the h3 class, is always in fully qualified

		final Elements inheritedBlocks = document.select("section.method-summary > div.inherited-list");

		for (Element inheritedBlock : inheritedBlocks) {
			final Element title = inheritedBlock.selectFirst("h3");
			if (title == null) throw new IllegalArgumentException();

			final Element superClassLink = title.selectFirst("a");
			if (superClassLink != null) {
				final ClassDocs superClassDocs = new ClassDocs(superClassLink.absUrl("href"));

				for (Element element : inheritedBlock.select("code > a")) {
					final HttpUrl hrefUrl = HttpUrl.get(element.absUrl("href"));

					String targetId = hrefUrl.fragment();

					//You can inherit a same method multiple times, it will show up multiple times in the docs
					// As the html is ordered such as the latest overridden method is shown, we can set the already existing doc to the newest one
					// Example: https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/AbstractCollection.html#method.summary
					final MethodDoc methodDoc = superClassDocs.methodDocs.get(targetId);
					methodDocs.put(methodDoc.getElementId(), methodDoc);
				}
			} else {
				final String[] titleSplit = title.text().split("\\s");
				final String fullName = titleSplit[titleSplit.length - 1]; //TODO should I still get the full qualification, and parse the package names ? in case the lib has classes the JDK has lol

				String className = Utils.getClassName(fullName);

				System.out.println(className);

				//TODO try to get from global cache
			}
		}

		//Try to find field details
		processDetailElements(document, ClassDetailType.FIELD, fieldElement -> fieldDocs.add(new FieldDocs(this, fieldElement)));

		//Try to find method details
		processDetailElements(document, ClassDetailType.METHOD, methodElement -> {
			final MethodDoc methodDocs = new MethodDoc(this, methodElement);

			this.methodDocs.put(methodDocs.getElementId(), methodDocs);
		});
	}

//	private boolean tryAddSuperMethodDoc(String targetId, MethodDoc superMethodDoc) {
//
//		for (int i = 0, methodDocsSize = methodDocs.size(); i < methodDocsSize; i++) {
//			MethodDoc methodDoc = methodDocs.get(i);
//
//			if (superMethodDoc.getElementId().equals(targetId)) {
//				methodDocs.set(i, superMethodDoc);
//
//				return true;
//			}
//		}
//
//		//If it's the first inherited method, just add it
//		if (superMethodDoc.getElementId().equals(targetId)) {
//			methodDocs.add(superMethodDoc);
//
//			return true;
//		}
//
//		return false;
//	}

	@Nullable
	public HTMLElement getDescriptionElement() {
		return descriptionElement;
	}

	@NotNull
	public String getClassName() {
		return className;
	}

	public List<HTMLElement> getTypeParameters() {
		return typeParameters;
	}

	@NotNull
	public List<FieldDocs> getFieldDocs() {
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
}
