package dev.freya02.doxxy.docs

import org.intellij.lang.annotations.Language

data class JavadocSources(
    private val sources: List<JavadocSource>,
) {

    operator fun contains(source: JavadocSource): Boolean = source in sources

    fun getByName(name: String): JavadocSource? {
        return sources.find { source -> source.name == name }
    }

    fun getByUrl(url: String): JavadocSource? {
        return sources.find { source ->
            url.startsWith(source.sourceUrl) || source.onlineURL != null && url.startsWith(source.onlineURL)
        }
    }
}

class JavadocSource(
    val name: String,
    val sourceUrl: String,
    val onlineURL: String?,
    val packageMatchers: List<PackageMatcher>,
) {

    val allClassesIndexURL: String = "$sourceUrl/allclasses-index.html"
    val constantValuesURL: String = "$sourceUrl/constant-values.html"

    fun isValidPackage(packageName: String): Boolean {
        return packageMatchers.any { it.matches(packageName) }
    }

    fun toEffectiveURL(url: String): String {
        if (onlineURL == null) return url

        return when {
            url.startsWith(sourceUrl) -> onlineURL + url.substring(sourceUrl.length)
            else -> url
        }
    }

    fun toOnlineURL(url: String): String? {
        if (onlineURL == null) return null

        return when {
            url.startsWith(sourceUrl) -> onlineURL + url.substring(sourceUrl.length)
            else -> url
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JavadocSource) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "JavadocSource(name='$name', packageMatchers=$packageMatchers, onlineURL=$onlineURL, sourceUrl='$sourceUrl')"
    }

    sealed interface PackageMatcher {
        fun matches(pkg: String): Boolean

        companion object {
            fun single(@Language("java", prefix = "import ", suffix = ".*;") pkg: String): PackageMatcher {
                return SinglePackageMatcher(pkg)
            }

            fun recursive(@Language("java", prefix = "import ", suffix = ".*;") pkg: String): PackageMatcher {
                return RecursivePackageMatcher(pkg)
            }
        }
    }

    private class SinglePackageMatcher(private val targetPkg: String) : PackageMatcher {
        override fun matches(pkg: String): Boolean = targetPkg == pkg

        override fun toString(): String = "SinglePackageMatcher(targetPkg='$targetPkg')"
    }

    private class RecursivePackageMatcher(private val targetPkg: String) : PackageMatcher {
        override fun matches(pkg: String): Boolean = pkg.startsWith(targetPkg)

        override fun toString(): String = "RecursivePackageMatcher(targetPkg='$targetPkg')"
    }
}