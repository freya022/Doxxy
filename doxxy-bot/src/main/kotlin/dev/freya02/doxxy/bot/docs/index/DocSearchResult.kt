package dev.freya02.doxxy.bot.docs.index

import io.github.freya022.botcommands.api.core.db.DBResult

data class DocSearchResult(val fullIdentifier: String, val humanIdentifier: String, val humanClassIdentifier: String, val returnType: String?) {
    val identifier: String = fullIdentifier.substringAfter('#')

    constructor(set: DBResult) : this(set["full_identifier"], set["human_identifier"], set["human_class_identifier"], set["return_type"])
}
