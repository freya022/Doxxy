package com.freya02.docs;

import com.freya02.bot.utils.DecomposedName;
import com.freya02.bot.utils.HTMLElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.StringJoiner;

public class MethodDoc {
	@NotNull private final ClassDoc classDocs;

	@NotNull private final String elementId;
	@NotNull private final String url;

	@NotNull private final String methodName;
	@NotNull private final String methodSignature;
	@NotNull private final String methodReturnType;
	@Nullable private final HTMLElement descriptionElement;

	@Nullable private final DetailToElementsMap detailToElementsMap;
	@Nullable private final List<HTMLElement> typeParameterElements;
	@Nullable private final List<HTMLElement> parameterElements;
	@Nullable private final HTMLElement returnsElement;
	@Nullable private final List<HTMLElement> throwsElements;
	@Nullable private final HTMLElement incubatingElement;
	@Nullable private final SeeAlso seeAlso;
	@Nullable private final HTMLElement defaultElement;
	@Nullable private final List<HTMLElement> specifiedByElements;
	@Nullable private final HTMLElement overridesElement;
	@Nullable private final HTMLElement since;

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

		final Element methodReturnTypeElement = element.selectFirst("div.member-signature > span.return-type");
		if (methodReturnTypeElement == null) throw new DocParseException();
		this.methodReturnType = methodReturnTypeElement.text();

		//Get method description
		final Element descriptionElement = element.selectFirst("section.detail > div.block");
		if (descriptionElement != null) {
			this.descriptionElement = new HTMLElement(descriptionElement);
		} else {
			this.descriptionElement = null;
		}

		//Need to parse the children of the <dl> tag in order to make a map of dt[class] -> List<Element>
		this.detailToElementsMap = new DetailToElementsMap(element);

		this.typeParameterElements = detailToElementsMap.get(DocDetailType.TYPE_PARAMETERS);
		this.parameterElements = detailToElementsMap.get(DocDetailType.PARAMETERS);
		this.returnsElement = detailToElementsMap.findFirst(DocDetailType.RETURNS);
		this.defaultElement = detailToElementsMap.findFirst(DocDetailType.DEFAULT);
		this.throwsElements = detailToElementsMap.get(DocDetailType.THROWS);
		this.incubatingElement = detailToElementsMap.findFirst(DocDetailType.INCUBATING);
		this.specifiedByElements = detailToElementsMap.get(DocDetailType.SPECIFIED_BY);
		this.overridesElement = detailToElementsMap.findFirst(DocDetailType.OVERRIDES);
		this.since = detailToElementsMap.findFirst(DocDetailType.SINCE);

		final HTMLElement seeAlsoElement = detailToElementsMap.findFirst(DocDetailType.SEE_ALSO);
		if (seeAlsoElement != null) {
			this.seeAlso = new SeeAlso(seeAlsoElement);
		} else {
			this.seeAlso = null;
		}

		detailToElementsMap.onParseFinished();
	}

	public DetailToElementsMap getDetailToElementsMap() {
		return detailToElementsMap;
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
	public String getMethodReturnType() {
		return methodReturnType;
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

	@Nullable
	public HTMLElement getIncubatingElement() {
		return incubatingElement;
	}

	@Nullable
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

	@Nullable
	public HTMLElement getDefaultElement() {
		return defaultElement;
	}
}
