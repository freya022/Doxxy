package com.freya02.bot.versioning.supplier;

public interface BuildToolDependencies {
	String getBCDependencyFormatString();

	String getJDA4DependencyFormatString();

	String getJDA5DependencyFormatString();

	String getJDA5JitpackDependencyFormatString();
}
