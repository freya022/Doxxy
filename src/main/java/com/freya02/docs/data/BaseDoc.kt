package com.freya02.docs.data;

import com.freya02.docs.HTMLElement;
import com.freya02.docs.HTMLElementList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public abstract class BaseDoc {
	@NotNull
	public abstract String getEffectiveURL();

	@NotNull
	public abstract HTMLElementList getDescriptionElements();

	@Nullable
	public abstract HTMLElement getDeprecationElement();

	@NotNull
	protected abstract DetailToElementsMap getDetailToElementsMap();

	@NotNull
	public List<DocDetail> getDetails(EnumSet<DocDetailType> includedTypes) {
		final List<DocDetail> details = new ArrayList<>();

		for (DocDetailType detailType : DocDetailType.values()) {
			if (includedTypes.contains(detailType)) {
				final DocDetail detail = getDetailToElementsMap().getDetail(detailType);
				if (detail == null) continue;

				details.add(detail);
			}
		}

		return details;
	}
}
