package com.freya02.bot.docs.index

data class DocSuggestion(val humanIdentifier: String, val identifier: String) {
    companion object {
        fun List<DocSearchResult>.mapToSuggestions(classPrefix: String? = null) = this.map {
            if (classPrefix != null) {
                DocSuggestion(it.humanClassIdentifier, "$classPrefix#${it.identifierOrFullIdentifier}")
            } else {
                DocSuggestion(it.humanClassIdentifier, it.identifierOrFullIdentifier)
            }
        }
    }
}