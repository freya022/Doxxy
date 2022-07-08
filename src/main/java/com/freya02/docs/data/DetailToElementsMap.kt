package com.freya02.docs.data

import com.freya02.botcommands.api.Logging
import com.freya02.docs.HTMLElement
import org.jsoup.nodes.Element
import java.util.*

private val LOGGER = Logging.getLogger()
private val warned: MutableSet<String> = hashSetOf()

class DetailToElementsMap private constructor(detailTarget: Element) {
    private val map: MutableMap<DocDetailType, MutableList<HTMLElement>> = EnumMap(DocDetailType::class.java)

    private inner class DetailsState(private val detailTarget: Element) {
        private var list: MutableList<HTMLElement>? = null

        fun newDetail(detailName: String) {
            val type = DocDetailType.parseType(detailName)

            @Suppress("LiftReturnOrAssignment")
            if (type == null) {
                if (warned.add(detailName))
                    LOGGER.warn("Unknown method detail type: '{}' at {}", detailName, detailTarget.baseUri())

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
        fun parseDetails(detailTarget: Element): DetailToElementsMap {
            return DetailToElementsMap(detailTarget)
        }
    }
}