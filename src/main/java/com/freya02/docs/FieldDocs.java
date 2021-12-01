package com.freya02.docs;

import com.freya02.bot.utils.HTMLElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;

public class FieldDocs {
	@NotNull private final String fieldName;
	@NotNull private final String fieldType;
	@Nullable private final HTMLElement descriptionElement;
	@NotNull private final ClassDocs classDocs;

	public FieldDocs(@NotNull ClassDocs classDocs, @NotNull Element element) {
		this.classDocs = classDocs;

		//Get field name
		final Element fieldNameElement = element.selectFirst("h4");
		if (fieldNameElement == null) throw new IllegalArgumentException();
		this.fieldName = fieldNameElement.text();

		//Get field type
		final Element fieldDeclarationElement = element.selectFirst("pre");
		if (fieldDeclarationElement == null) throw new IllegalArgumentException();

		final String fieldDeclaration = fieldDeclarationElement.text().replace((char) 160, ' ');
		final int secondLastSpaceIndex = fieldDeclaration.lastIndexOf(' ', fieldDeclaration.length() - 6);
		this.fieldType = fieldDeclaration.substring(secondLastSpaceIndex, fieldDeclaration.lastIndexOf(' '));

		//Get field description
		final Element descriptionElement = element.selectFirst("div");
		if (descriptionElement != null) {
			this.descriptionElement = new HTMLElement(descriptionElement);
		} else {
			this.descriptionElement = null;
		}
	}

	@NotNull
	public String getFieldName() {
		return fieldName;
	}

	@NotNull
	public String getFieldType() {
		return fieldType;
	}

	@Nullable
	public HTMLElement getDescriptionElement() {
		return descriptionElement;
	}

	@Override
	public String toString() {
		return fieldType + " " + fieldName + " : " + descriptionElement;
	}
}
