package dev.freya02.doxxy.docs.sections

import dev.freya02.doxxy.docs.HTMLElement
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Element
import java.util.*

class DetailToElementsMap private constructor(detailTarget: Element) {
    private val map: MutableMap<DocDetail.Type, MutableList<HTMLElement>> = EnumMap(DocDetail.Type::class.java)

    private inner class DetailsState(private val detailTarget: Element) {
        private var list: MutableList<HTMLElement>? = null

        fun newDetail(detailName: String) {
            val type = DocDetail.Type.parseType(detailName)

            @Suppress("LiftReturnOrAssignment")
            if (type == null) {
                if (warned.add(detailName))
                    logger.warn { "Unknown method detail type: '$detailName' at ${detailTarget.baseUri()}" }

                list = null
            } else {
                list = map.getOrPut(type) { arrayListOf() }
            }
        }

        fun pushDetail(supplier: () -> HTMLElement) {
            list?.add(supplier())
        }
    }

    init {
        val detailsState = DetailsState(detailTarget)

        for (element in detailTarget.select("dl.notes")) {
            for (child in element.children()) {
                val tagName = child.tag().normalName()
                if (tagName == "dt") {
                    val detailName = child.text()

                    detailsState.newDetail(detailName)
                } else if (tagName == "dd") {
                    detailsState.pushDetail { HTMLElement.wrap(child) }
                }
            }
        }
    }

    fun getDetail(detailType: DocDetail.Type): DocDetail? {
        val elements = map[detailType] ?: return null
        return DocDetail(detailType, elements)
    }

    internal companion object {
        private val logger = KotlinLogging.logger { }
        private val warned: MutableSet<String> = hashSetOf()

        internal fun parseDetails(detailTarget: Element): DetailToElementsMap {
            return DetailToElementsMap(detailTarget)
        }
    }
}