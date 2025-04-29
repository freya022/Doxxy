package dev.freya02.doxxy.bot.docs.index

import java.util.*

class DocTypes(private val set: Set<DocType>): Set<DocType> by set {
    constructor(vararg types: DocType) : this(types.toSet())

    fun getRaw(): Long = this.fold(0) { acc, type -> acc or type.raw }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocTypes

        if (set != other.set) return false

        return true
    }

    override fun hashCode(): Int {
        return set.hashCode()
    }

    companion object {
        val CLASS = DocTypes(DocType.CLASS)
        val METHOD = DocTypes(DocType.METHOD)
        val FIELD = DocTypes(DocType.FIELD)
        val ANY = DocTypes(DocType.CLASS, DocType.METHOD, DocType.FIELD)

        val IDENTIFIERS = DocTypes(DocType.FIELD, DocType.METHOD)

        fun fromRaw(raw: Long): DocTypes {
            val set = EnumSet.noneOf(DocType::class.java)
            for (type in DocType.entries) {
                if ((raw and type.raw) == type.raw) {
                    set.add(type)
                }
            }

            return DocTypes(set)
        }
    }
}
