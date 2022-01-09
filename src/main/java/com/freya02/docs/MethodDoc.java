package com.freya02.docs;

import com.freya02.bot.utils.DecomposedName;
import com.freya02.bot.utils.HTMLElement;
import com.freya02.botcommands.api.Logging;
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
	@NotNull private final String url;

	@NotNull private final String methodName;
	@NotNull private final String methodSignature;
	@Nullable private final HTMLElement descriptionElement;

	@Nullable private final Map<DocDetailType, List<HTMLElement>> detailToElementsMap;
	@Nullable private final List<HTMLElement> typeParameterElements;
	@Nullable private final List<HTMLElement> parameterElements;
	@Nullable private final HTMLElement returnsElement;
	@Nullable private final List<HTMLElement> throwsElements;
	@Nullable private final HTMLElement incubatingElement;
	@Nullable private final SeeAlso seeAlso;

	public MethodDoc(@NotNull ClassDoc classDocs, @NotNull Element element) {
		this.classDocs = classDocs;

		this.elementId = element.id();
		this.url = classDocs.getURL() + '#' + elementId;

		//Get method name
		final Element methodNameElement = element.selectFirst("h3");
		if (methodNameElement == null) throw new DocParseException();
		this.methodName = methodNameElement.text();

		//Get method signature
		final Element methodSignatureElement = element.selectFirst("div.member-signature");
		if (methodSignatureElement == null) throw new DocParseException();

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

			this.typeParameterElements = detailToElementsMap.get(DocDetailType.TYPE_PARAMETERS);
			this.parameterElements = detailToElementsMap.get(DocDetailType.PARAMETERS);
			this.returnsElement = findFirst(detailToElementsMap, DocDetailType.RETURNS);
			this.throwsElements = detailToElementsMap.get(DocDetailType.THROWS);
			this.incubatingElement = findFirst(detailToElementsMap, DocDetailType.INCUBATING);

			final List<HTMLElement> seeAlsoElements = detailToElementsMap.get(DocDetailType.SEE_ALSO);
			if (seeAlsoElements != null) {
				this.seeAlso = new SeeAlso(seeAlsoElements.get(0));
			} else {
				this.seeAlso = null;
			}
		} else {
			this.detailToElementsMap = null;

			this.typeParameterElements = null;
			this.parameterElements = null;
			this.returnsElement = null;
			this.throwsElements = null;
			this.incubatingElement = null;
			this.seeAlso = null;
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

	@NotNull
	public String getSimpleSignature() {
		final StringBuilder simpleSignatureBuilder = new StringBuilder();

		final int index = elementId.indexOf('(');
		simpleSignatureBuilder.append(elementId, 0, index);

		final StringJoiner parameterJoiner = new StringJoiner(", ", "(", ")");
		final String[] parameters = elementId.substring(index + 1, elementId.length() - 1).split(",");
		for (String parameter : parameters) {
			if (parameter.isBlank()) continue;

			final String className = DecomposedName.getSimpleClassName(parameter.trim());

			parameterJoiner.add(className);
		}

		simpleSignatureBuilder.append(parameterJoiner);

		return simpleSignatureBuilder.toString();
	}

	@Nullable
	public HTMLElement getDescriptionElement() {
		return descriptionElement;
	}

	@Nullable
	public HTMLElement getReturnsElement() {
		return returnsElement;
	}

	@Nullable
	public List<HTMLElement> getParameterElements() {
		return parameterElements;
	}

	@Nullable
	public List<HTMLElement> getTypeParameterElements() {
		return typeParameterElements;
	}

	@Nullable
	public List<HTMLElement> getThrowsElements() {
		return throwsElements;
	}

	public HTMLElement getIncubatingElement() {
		return incubatingElement;
	}

	public SeeAlso getSeeAlso() {
		return seeAlso;
	}

	@Override
	public String toString() {
		return methodSignature + " : " + descriptionElement;
	}

	public String getURL() {
		return url;
	}
}
