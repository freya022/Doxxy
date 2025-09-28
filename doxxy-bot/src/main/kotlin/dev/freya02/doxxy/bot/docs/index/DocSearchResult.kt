package dev.freya02.doxxy.bot.docs.index

import io.github.freya022.botcommands.api.core.db.DBResult

data class DocSearchResult(val fullIdentifier: String, val humanClassIdentifier: String, val returnType: String?) {
    val identifier: String = fullIdentifier.substringAfter('#')
    val humanIdentifier: String get() = humanClassIdentifier.substringAfter('#')

    // TODO rename properties
    constructor(set: DBResult) : this(set["qualified_member"], set["display_qualified_member"], set["return_type"])
}
