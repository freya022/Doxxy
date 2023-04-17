package com.freya02.docs.data

import com.freya02.docs.HTMLElement
import mu.KotlinLogging
import org.jsoup.nodes.Element
import java.util.*

class DetailToElementsMap private constructor(detailTarget: Element) {
    private val map: MutableMap<DocDetailType, MutableList<HTMLElement>> = EnumMap(DocDetailType::class.java)

    private inner class DetailsState(private val detailTarget: Element) {
        private var list: MutableList<HTMLElement>? = null

        fun newDetail(detailName: String) {
            val type = DocDetailType.parseType(detailName)

            @Suppress("LiftReturnOrAssignment")
            if (type == null) {
                if (warned.add(detailName))
                    logger.warn("Unknown method detail type: '{}' at {}", detailName, detailTarget.baseUri())

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

    fun getDetail(detailType: DocDetailType): DocDetail? {
        val elements = map[detailType] ?: return null
        return DocDetail(detailType, elements)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val warned: MutableSet<String> = hashSetOf()

        fun parseDetails(detailTarget: Element): DetailToElementsMap {
            return DetailToElementsMap(detailTarget)
        }
    }
}