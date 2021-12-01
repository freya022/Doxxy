package com.freya02.bot.docs;

import com.freya02.bot.utils.DecomposedName;
import com.freya02.bot.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ClassReference {
	private @Nullable String packageName;
	private final @NotNull String className;
	private @Nullable String link;

	public ClassReference(@Nullable String packageName, @NotNull String className, @Nullable String link) {
		this.packageName = packageName;
		this.className = className;
		this.link = link == null ? null : Utils.fixJDKUrl(link);
	}

	public void setPackageName(@Nullable String packageName) {
		this.packageName = packageName;
	}

	public void setLink(@Nullable String link) {
		this.link = link;
	}

	public static ClassReference generate(@NotNull String fullName, @Nullable String link) {
		final DecomposedName decomposition = DecomposedName.getDecomposition(fullName);

		return new ClassReference(decomposition.packageName(), decomposition.className(), link);
	}

	public String getFullName() {
		if (packageName == null) return className;

		return packageName + '.' + className;
	}

	public @Nullable String packageName() {return packageName;}

	public @NotNull String className() {return className;}

	public @Nullable String link() {return link;}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (ClassReference) obj;
		return Objects.equals(this.packageName, that.packageName) &&
				Objects.equals(this.className, that.className) &&
				Objects.equals(this.link, that.link);
	}

	@Override
	public int hashCode() {
		return Objects.hash(packageName, className, link);
	}

	@Override
	public String toString() {
		return "ClassReference[" +
				"packageName=" + packageName + ", " +
				"className=" + className + ", " +
				"link=" + link + ']';
	}

}
