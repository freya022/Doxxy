package com.freya02.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.botcommands.internal.Logging;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.util.*;

public class MethodDoc {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Set<String> warned = Collections.synchronizedSet(new HashSet<>());

	@NotNull private final ClassDoc classDocs;

	@NotNull private final String elementId;

	@NotNull private final String methodName;
	@NotNull private final String methodSignature;
	@Nullable private final HTMLElement descriptionElement;

	@Nullable private final Map<DocDetailType, List<HTMLElement>> detailToElementsMap;
	@Nullable private final List<HTMLElement> typeParametersDoc;
	@Nullable private final List<HTMLElement> parametersDoc;
	@Nullable private final HTMLElement returnsDoc;
	@Nullable private final List<HTMLElement> throwsDocs;
	@Nullable private final HTMLElement incubatingDocs;

	public MethodDoc(@NotNull ClassDoc classDocs, @NotNull Element element) {
		this.classDocs = classDocs;

		this.elementId = element.id();

		//Get method name
		final Element methodNameElement = element.selectFirst("h3");
		if (methodNameElement == null) throw new IllegalArgumentException();
		this.methodName = methodNameElement.text();

		//Get method signature
		final Element methodSignatureElement = element.selectFirst("div.member-signature");
		if (methodSignatureElement == null) throw new IllegalArgumentException();

		this.methodSignature = methodSignatureElement.text();

		//Get method description
		final Element descriptionElement = element.selectFirst("section.detail > div.block");
		if (descriptionElement != null) {
			this.descriptionElement = new HTMLElement(descriptionElement);
		} else {
			this.descriptionElement = null;
		}

		//Need to parse the children of the <dl> tag in order to make a map of dt[class] -> List<Element>
		final Element detailListElement = element.selectFirst("dl");
		if (detailListElement != null) {
			this.detailToElementsMap = getDetailToElementsMap(detailListElement);

			this.typeParametersDoc = detailToElementsMap.get(DocDetailType.TYPE_PARAMETERS);
			this.parametersDoc = detailToElementsMap.get(DocDetailType.PARAMETERS);
			this.returnsDoc = findFirst(detailToElementsMap, DocDetailType.RETURNS);
			this.throwsDocs = detailToElementsMap.get(DocDetailType.THROWS);
			this.incubatingDocs = findFirst(detailToElementsMap, DocDetailType.INCUBATING);
		} else {
			this.detailToElementsMap = null;

			this.typeParametersDoc = null;
			this.parametersDoc = null;
			this.returnsDoc = null;
			this.throwsDocs = null;
			this.incubatingDocs = null;
		}
	}

	public Map<DocDetailType, List<HTMLElement>> getDetailToElementsMap() {
		return detailToElementsMap;
	}

	@Nullable
	private HTMLElement findFirst(Map<DocDetailType, List<HTMLElement>> detailToElementsMap, DocDetailType name) {
		final List<HTMLElement> list = detailToElementsMap.get(name);
		if (list == null || list.isEmpty()) return null;

		return list.get(0);
	}

	@NotNull
	static Map<DocDetailType, List<HTMLElement>> getDetailToElementsMap(@NotNull Element detailListElement) {
		final Map<DocDetailType, List<HTMLElement>> detailClassNameToElementsMap = new HashMap<>();

		List<HTMLElement> list = null;
		for (Element child : detailListElement.children()) {
			final String tagName = child.tag().normalName();

			if (tagName.equals("dt")) {
				final String detailName = child.text();
				final DocDetailType type = DocDetailType.parseType(detailName);
				if (type == null) {
					if (warned.add(detailName)) {
						LOGGER.warn("Unknown method detail type: '{}'", detailName);
					}

					list = null;
				} else {
					list = detailClassNameToElementsMap.computeIfAbsent(type, s -> new ArrayList<>());
				}
			} else if (tagName.equals("dd") && list != null) {
				list.add(new HTMLElement(child));
			}
		}

		return detailClassNameToElementsMap;
	}

	@NotNull
	public String getElementId() {
		return elementId;
	}

	@NotNull
	public ClassDoc getClassDocs() {
		return classDocs;
	}

	@NotNull
	public String getMethodName() {
		return methodName;
	}

	@NotNull
	public String getMethodSignature() {
		return methodSignature;
	}

	@Nullable
	public HTMLElement getDescriptionElement() {
		return descriptionElement;
	}

	@Nullable
	public HTMLElement getReturnsDoc() {
		return returnsDoc;
	}

	@Nullable
	public List<HTMLElement> getParametersDoc() {
		return parametersDoc;
	}

	@Nullable
	public List<HTMLElement> getTypeParametersDoc() {
		return typeParametersDoc;
	}

	@Nullable
	public List<HTMLElement> getThrowsDocs() {
		return throwsDocs;
	}

	public HTMLElement getIncubatingDocs() {
		return incubatingDocs;
	}

	@Override
	public String toString() {
		return methodSignature + " : " + descriptionElement;
	}
}
