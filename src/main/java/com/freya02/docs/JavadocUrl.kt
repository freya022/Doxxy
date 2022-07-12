package com.freya02.docs

import com.freya02.docs.data.TargetType
import com.freya02.docs.data.TargetType.Companion.fromFragment
import okhttp3.HttpUrl.Companion.toHttpUrl

class JavadocUrl private constructor(val className: String, val fragment: String?, val targetType: TargetType) {
    companion object {
        fun fromURL(url: String): JavadocUrl {
            url.toHttpUrl().let { httpUrl ->
                val lastFragment = httpUrl.pathSegments.last()
                val className = lastFragment.dropLast(5) //Remove .html
                val fragment = httpUrl.fragment

                return JavadocUrl(className, fragment, fromFragment(fragment))
            }
        }
    }
}