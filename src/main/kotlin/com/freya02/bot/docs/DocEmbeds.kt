package com.freya02.bot.docs

import com.freya02.bot.utils.HttpUtils.doesStartByLocalhost
import com.freya02.docs.data.*
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import dev.minn.jda.ktx.messages.EmbedBuilder
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.util.*

object DocEmbeds {
    private const val DESCRIPTION_MAX_LENGTH = 2048 //Description max length / 2 basically, otherwise embeds are HUGE

    //SEE_ALSO not needed, already added manually
    private val includedTypes: EnumSet<DocDetailType> = EnumSet.of( //SEE_ALSO not needed, already added manually
        DocDetailType.PARAMETERS,
        DocDetailType.TYPE_PARAMETERS,
        DocDetailType.RETURNS,
        DocDetailType.SPECIFIED_BY,
        DocDetailType.OVERRIDES,
        DocDetailType.INCUBATING,
        DocDetailType.DEFAULT,
        DocDetailType.THROWS
    )

    fun toEmbed(doc: ClassDoc): EmbedBuilder {
        return EmbedBuilder {
            title = doc.docTitleElement.targetElement.text()
            url = getDocURL(doc)

            builder.addDocDescription(doc)
            builder.addDocDeprecation(doc)

            val enumConstants = doc.enumConstants
            if (enumConstants.isNotEmpty()) {
                val valuesStr = enumConstants
                    .take(10)
                    .joinToString("`\n`", "`", "`") { it.fieldName }

                builder.addDocField(
                    "Enum values:",
                    valuesStr + if (enumConstants.size > 10) "\n... and more ..." else "",
                    false,
                    getDocURL(doc)
                )
            }

            val annotationElements = doc.annotationElements
            if (annotationElements.isNotEmpty()) {
                val fieldsStr = annotationElements
                    .take(10)
                    .joinToString("`\n`", "`", "`") { "#" + it.methodName + "()" }

                builder.addDocField(
                    "Annotation fields:",
                    fieldsStr + if (annotationElements.size > 10) "\n... and more ..." else "",
                    false,
                    getDocURL(doc)
                )
            }

            builder.addDocDetails(doc, includedTypes)

            builder.addSeeAlso(doc.seeAlso, getDocURL(doc))
        }.builder
    }

    fun toEmbed(classDoc: ClassDoc, methodDoc: MethodDoc): EmbedBuilder {
        return EmbedBuilder {
            var title = methodDoc.getSimpleAnnotatedSignature(classDoc)
            if (title.length > MessageEmbed.TITLE_MAX_LENGTH) {
                title = "%s#%s : %s - [full signature on online docs]".format(
                    methodDoc.classDocs.className,
                    methodDoc.methodName,
                    methodDoc.methodReturnType
                )
            }

            builder.setTitle(title, getDocURL(methodDoc))

            //Should use that but JB annotations are duplicated, bruh momentum
//		    builder.setTitle(methodDoc.getMethodSignature(), methodDoc.getURL());
            if (classDoc != methodDoc.classDocs) {
                builder.setDescription("**Inherited from ${methodDoc.classDocs.className}**\n\n")
            }

            builder.addDocDescription(methodDoc)
            builder.addDocDeprecation(methodDoc)
            builder.addDocDetails(methodDoc, includedTypes)
            builder.addSeeAlso(methodDoc.seeAlso, getDocURL(methodDoc))
        }.builder
    }

    fun toEmbed(classDoc: ClassDoc, fieldDoc: FieldDoc): EmbedBuilder {
        return EmbedBuilder {
            title = classDoc.className + " : " + fieldDoc.simpleSignature
            url = getDocURL(fieldDoc)

            if (classDoc != fieldDoc.classDocs) {
                description = "**Inherited from ${fieldDoc.classDocs.className}**\n\n"
            }

            builder.addDocDescription(fieldDoc)
            builder.addDocDeprecation(fieldDoc)
            builder.addDocDetails(fieldDoc, includedTypes)
            builder.addSeeAlso(fieldDoc.seeAlso, getDocURL(fieldDoc))
        }.builder
    }

    private fun getDocURL(doc: BaseDoc): String? {
        return if (doesStartByLocalhost(doc.effectiveURL)) null else doc.effectiveURL
    }

    private fun EmbedBuilder.addDocDescription(doc: BaseDoc) {
        val descriptionElement = doc.descriptionElements
        if (descriptionElement.isNotEmpty()) {
            appendDescription(
                getDescriptionValue(
                    descriptionBuilder.length,
                    descriptionElement.toMarkdown("\n\n"), //Description blocks are separated like paragraphs, unlike details such as "Specified by"
                    getDocURL(doc)
                )
            )
        } else {
            appendDescription("No description")
        }
    }

    private fun EmbedBuilder.addDocDeprecation(doc: BaseDoc) {
        val deprecationElement = doc.deprecationElement
        if (deprecationElement != null) {
            this.addDocField(
                "Deprecated",
                deprecationElement.asMarkdown,
                false,
                getDocURL(doc)
            )
        }
    }

    private fun EmbedBuilder.addDocDetails(doc: BaseDoc, excludedTypes: EnumSet<DocDetailType>) {
        val details = doc.getDetails(excludedTypes)
        for (detail in details) {
            addDocField(
                detail.detailString,
                detail.toMarkdown("\n"),
                false,
                getDocURL(doc)
            )
        }
    }

    private fun getDescriptionValue(currentLength: Int, descriptionValue: String, onlineTarget: String?): String =
        when {
            descriptionValue.length + currentLength > DESCRIPTION_MAX_LENGTH -> {
                when (onlineTarget) {
                    null -> "Description is too long. Please look at the docs in your IDE"
                    else -> "Description is too long. Please look at [the online docs]($onlineTarget)"
                }
            }
            else -> descriptionValue
        }

    private fun EmbedBuilder.addDocField(
        fieldName: String,
        fieldValue: String,
        inline: Boolean,
        onlineDocs: String?
    ) {
        when {
            fieldValue.length > MessageEmbed.VALUE_MAX_LENGTH -> {
                when (onlineDocs) {
                    null -> addField(
                        fieldName,
                        "This section is too long" + ". Please look at the docs in your IDE",
                        inline
                    )
                    else -> addField(
                        fieldName,
                        "This section is too long. Please look at [the online docs]($onlineDocs)",
                        inline
                    )
                }
            }
            else -> addField(fieldName, fieldValue, inline)
        }
    }

    private fun EmbedBuilder.addSeeAlso(seeAlso: SeeAlso?, onlineDocs: String?) {
        if (seeAlso == null) return

        val seeAlsoMd = seeAlso.getReferences().joinToString(", ") { ref: SeeAlsoReference ->
            return@joinToString when {
                doesStartByLocalhost(ref.link) -> ref.text
                else -> "[" + ref.text + "](" + ref.link + ")"
            }
        }
        addDocField(
            "See Also",
            seeAlsoMd,
            false,
            onlineDocs
        )
    }
}