package com.freya02.docs.data;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.docs.DocParseException;
import com.freya02.docs.DocUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

public class MethodDoc extends BaseDoc {
	@NotNull private final ClassDoc classDocs;

	@NotNull private final String elementId;
	@NotNull private final String url;

	@NotNull private final String methodName;
	@NotNull private final String methodSignature;
	@NotNull private final String methodReturnType;
	@Nullable private final HTMLElement descriptionElement;
	@Nullable private final HTMLElement deprecationElement;

	@NotNull private final DetailToElementsMap detailToElementsMap;

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

		final Element deprecationElement = element.selectFirst("section.detail > div.deprecation-block");
		if (deprecationElement != null) {
			this.deprecationElement = new HTMLElement(deprecationElement);
		} else {
			this.deprecationElement = null;
		}

		//Need to parse the children of the <dl> tag in order to make a map of dt[class] -> List<Element>
		this.detailToElementsMap = DetailToElementsMap.parseDetails(element);

		final DocDetail seeAlsoDetail = detailToElementsMap.getDetail(DocDetailType.SEE_ALSO);
		if (seeAlsoDetail != null) {
			this.seeAlso = new SeeAlso(seeAlsoDetail);
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

	@Override
	@NotNull
	public String getURL() {
		return url;
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
}
