package dev.freya02.doxxy.bot.docs.index

import io.github.freya022.botcommands.api.core.db.DBResult

data class DocSearchResult(val qualifiedMember: String, val displayQualifiedMember: String, val returnType: String?) {
    val memberSignature: String = qualifiedMember.substringAfter('#')
    val displayMemberSignature: String get() = displayQualifiedMember.substringAfter('#')

    constructor(set: DBResult) : this(set["qualified_member"], set["display_qualified_member"], set["return_type"])
}
