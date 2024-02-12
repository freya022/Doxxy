package com.freya02.bot.docs.index

//TODO replace with DocSearchResult
data class DocSuggestion(val humanIdentifier: String, val fullIdentifier: String) {
    companion object {
        fun List<DocSearchResult>.mapToSuggestions() = this.map {
            DocSuggestion(it.humanClassIdentifier, it.fullIdentifier)
        }
    }
}
