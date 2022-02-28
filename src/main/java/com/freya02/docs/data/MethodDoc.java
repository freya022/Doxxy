package com.freya02.docs.data;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.docs.DocParseException;
import com.freya02.docs.DocUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.stream.Collectors;

public class MethodDoc extends BaseDoc {
	@NotNull private final ClassDoc classDocs;
	@NotNull private final ClassDetailType classDetailType;

	@NotNull private final String elementId;

	@Nullable private final String methodAnnotations;
	@NotNull private final String methodName;
	@NotNull private final String methodSignature;
	@Nullable private final String methodParameters;
	@NotNull private final String methodReturnType;
	@Nullable private final HTMLElement descriptionElement;
	@Nullable private final HTMLElement deprecationElement;

	@NotNull private final DetailToElementsMap detailToElementsMap;

	@Nullable private final SeeAlso seeAlso;

	public MethodDoc(@NotNull ClassDoc classDoc, @NotNull ClassDetailType classDetailType, @NotNull Element element) {
		this.classDocs = classDoc;
		this.classDetailType = classDetailType;

		this.elementId = element.id();

		//Get method name
		final Element methodNameElement = element.selectFirst("h3");
		if (methodNameElement == null) throw new DocParseException();
		this.methodName = methodNameElement.text();

		//Get method signature
		final Element methodSignatureElement = element.selectFirst("div.member-signature");
		if (methodSignatureElement == null) throw new DocParseException();

		this.methodSignature = methodSignatureElement.text();

		final Element methodAnnotationsElement = element.selectFirst("div.member-signature > span.annotations");
		this.methodAnnotations = methodAnnotationsElement == null ? null : methodAnnotationsElement.text();

		final Element methodParametersElement = element.selectFirst("div.member-signature > span.parameters");
		this.methodParameters = methodParametersElement == null ? null : methodParametersElement.text();

		final Element methodReturnTypeElement = element.selectFirst("div.member-signature > span.return-type");
		if (methodReturnTypeElement == null) throw new DocParseException();
		this.methodReturnType = methodReturnTypeElement.text();

		//Get method description
		this.descriptionElement = HTMLElement.tryWrap(Jsoup.parseBodyFragment(
				element.select("section.detail > div.block").stream()
						.map(Element::outerHtml) //Description blocks could be split because of inheritance labels, we need to merge them
						.collect(Collectors.joining()).trim(),
				element.baseUri()
		));

		//Get method possible's deprecation
		this.deprecationElement = HTMLElement.tryWrap(element.selectFirst("section.detail > div.deprecation-block"));

		//Need to parse the children of the <dl> tag in order to make a map of dt[class] -> List<Element>
		this.detailToElementsMap = DetailToElementsMap.parseDetails(element);

		final DocDetail seeAlsoDetail = detailToElementsMap.getDetail(DocDetailType.SEE_ALSO);
		if (seeAlsoDetail != null) {
			this.seeAlso = new SeeAlso(classDoc.getSource(), seeAlsoDetail);
		} else {
			this.seeAlso = null;
		}
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
	public ClassDetailType getClassDetailType() {
		return classDetailType;
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
		return DocUtils.getSimpleSignature(elementId);
	}

	@NotNull
	public String getSimpleAnnotatedSignature() {
		return DocUtils.getSimpleAnnotatedSignature(this);
	}

	@Override
	@NotNull
	public String getEffectiveURL() {
		return classDocs.getEffectiveURL() + '#' + elementId;
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

	@Override
	public String toString() {
		return methodSignature + " : " + descriptionElement;
	}

	@Nullable
	public String getMethodParameters() {
		return methodParameters;
	}

	@Nullable
	public String getMethodAnnotations() {
		return methodAnnotations;
	}
}
