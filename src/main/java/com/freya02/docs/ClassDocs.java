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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClassDocs {
	@Nullable private final HTMLElement descriptionElement;
	@NotNull private final String className;
	@Nullable private final List<HTMLElement> typeParameters;

	private final List<FieldDocs> fieldDocs = new ArrayList<>();
	private final List<MethodDocs> methodDocs = new ArrayList<>();

	public ClassDocs(@NotNull String url) throws IOException {
		final Document document = Utils.getDocument(url);

		//Get class name
		final List<String> segments = HttpUrl.get(url).pathSegments();
		this.className = segments.get(segments.size() - 1).substring(0, segments.get(segments.size() - 1).length() - 5);

		//Get class description
		final Element descriptionElement = document.selectFirst("body > main > div.contentContainer > div.description > ul > li > div");
		if (descriptionElement != null) {
			this.descriptionElement = new HTMLElement(descriptionElement);
		} else {
			this.descriptionElement = null;
		}

		//Get class type parameters if they exist
		final Element detailListElement = document.selectFirst("body > div.flex-box > div > main > section.description > dl");
		if (detailListElement != null) {
			final Map<DocDetailType, List<HTMLElement>> map = MethodDocs.getDetailToElementsMap(detailListElement);
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

		final Elements inheritedBlocks = document.select("body > main > div.contentContainer > div.summary > ul > li > section > ul > li > ul > li");

		for (Element inheritedBlock : inheritedBlocks) {
			final Element title = inheritedBlock.selectFirst("h3");
			if (title == null) throw new IllegalArgumentException();

			final Element superClassLink = title.selectFirst("a");
			if (superClassLink != null) {
				final ClassDocs superClassDocs = new ClassDocs(Utils.fixJDKUrl(superClassLink.absUrl("href")));

				//Need to use JDK 16 docs
				//section contains method id with precise parameters
				//links are correctly formatted
				//uses modern html overall

				//Need to filter method that are not inherited (and so are supposedly overridden)
				methodDocs.addAll(superClassDocs.methodDocs);

			} else {
				final String[] titleSplit = title.text().split("\\s");
				final String fullName = titleSplit[titleSplit.length - 1]; //TODO should I still get the full qualification, and parse the package names ? in case the lib has classes the JDK has lol

				String className = getClassName(fullName);

				System.out.println(className);

				//TODO try to get from global cache
			}
		}

		//Try to find field details
		processDetailElements(document, ClassDetailType.FIELD, fieldElement -> fieldDocs.add(new FieldDocs(this, fieldElement)));

		//Try to find method details
		processDetailElements(document, ClassDetailType.METHOD, methodElement -> methodDocs.add(new MethodDocs(this, methodElement)));
	}

	@NotNull
	private String getClassName(String fullName) {
		for (int i = 0, length = fullName.length(); i < length; i++) {
			final char c = fullName.charAt(i);

			if (Character.isUpperCase(c)) {
				return fullName.substring(i);
			}
		}

		throw new IllegalArgumentException("Could not get glass name from '" + fullName + "'");
	}

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

	@NotNull
	public List<MethodDocs> getMethodDocs() {
		return methodDocs;
	}

	private void processDetailElements(@NotNull Document document, @NotNull ClassDetailType detailType, Consumer<@NotNull Element> callback) {
		final String detailTitleName = detailType.getDetailTitleName();

		//Get main blocks to determine what details are available (field, constructor (constr), method)
		final Elements blockTitleElements = document.select("body > main > div.contentContainer > div.details > ul > li > section > ul > li > h3");

		for (Element blockTitleElement : blockTitleElements) {

			//Find the block with the same name as provided
			if (blockTitleElement.text().equals(detailTitleName)) {
				//Get the parent element since we found the title for the block, not the block itself
				final Element parent = blockTitleElement.parent();
				if (parent == null)
					throw new IllegalArgumentException("No parent for detail block " + detailTitleName + ", really weird, cannot be a root node");

				//We then look for all the sub-blocks (so fields / constructors / methods) by their title (h3)
				final Elements titleElements = parent.select("ul > li > h4:nth-child(1)");

				//Iterate on all titles and process their parent using the callback
				for (Element titleElement : titleElements) {
					final Element titleParent = titleElement.parent();
					if (titleParent == null)
						throw new IllegalArgumentException("Title element shouldn't been a root element");

					callback.accept(titleParent);
				}

				return; //findFirst equivalent
			}
		}
	}

	@Override
	public String toString() {
		return "%s : %d fields, %d methods%s".formatted(className, fieldDocs.size(), methodDocs.size(), descriptionElement == null ? "" : " : " + descriptionElement.getTargetElement().text());
	}
}
