package com.freya02.bot.docs.index

import com.freya02.botcommands.api.core.db.DBResult

data class DocSearchResult(val fullIdentifier: String, val humanIdentifier: String, val humanClassIdentifier: String, val returnType: String?) {
    val identifier: String = fullIdentifier.substringAfter('#')

    constructor(set: DBResult) : this(set["full_identifier"], set["human_identifier"], set["human_class_identifier"], set["return_type"])
}
