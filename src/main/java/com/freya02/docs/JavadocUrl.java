package com.freya02.docs;

import com.freya02.docs.data.TargetType;
import okhttp3.HttpUrl;

import java.util.List;

public class JavadocUrl {
	private final String className;
	private final String fragment;
	private final TargetType targetType;

	private JavadocUrl(String className, String fragment, TargetType targetType) {
		this.className = className;
		this.fragment = fragment;
		this.targetType = targetType;
	}

	public static JavadocUrl fromURL(String url) {
		final HttpUrl httpUrl = HttpUrl.get(url);

		final List<String> segments = httpUrl.pathSegments();
		final String lastFragment = segments.get(segments.size() - 1);

		final String className = lastFragment.substring(0, lastFragment.length() - 5); //Remove .html

		final String fragment = httpUrl.fragment();

		return new JavadocUrl(className, fragment, TargetType.fromFragment(fragment));
	}

	public String getClassName() {
		return className;
	}

	public String getFragment() {
		return fragment;
	}

	public TargetType getTargetType() {
		return targetType;
	}
}
