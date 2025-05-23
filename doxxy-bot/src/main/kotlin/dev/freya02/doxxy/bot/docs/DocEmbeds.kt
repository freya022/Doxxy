package dev.freya02.doxxy.bot.docs

import dev.freya02.doxxy.bot.docs.render.toMarkdown
import dev.freya02.doxxy.bot.utils.HttpUtils.doesStartByLocalhost
import dev.freya02.doxxy.docs.declarations.AbstractJavadoc
import dev.freya02.doxxy.docs.declarations.JavadocClass
import dev.freya02.doxxy.docs.declarations.JavadocField
import dev.freya02.doxxy.docs.declarations.JavadocMethod
import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.sections.SeeAlso
import dev.freya02.doxxy.docs.sections.SeeAlso.SeeAlsoReference
import dev.minn.jda.ktx.messages.InlineEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.util.*

object DocEmbeds {
    private const val DESCRIPTION_MAX_LENGTH = 2048 //Description max length / 2 basically, otherwise embeds are HUGE

    //SEE_ALSO not needed, already added manually
    private val includedTypes: EnumSet<DocDetail.Type> = EnumSet.of( //SEE_ALSO not needed, already added manually
        DocDetail.Type.PARAMETERS,
        DocDetail.Type.TYPE_PARAMETERS,
        DocDetail.Type.RETURNS,
        DocDetail.Type.SPECIFIED_BY,
        DocDetail.Type.OVERRIDES,
        DocDetail.Type.INCUBATING,
        DocDetail.Type.DEFAULT,
        DocDetail.Type.THROWS
    )

    fun toEmbed(doc: JavadocClass): MessageEmbed = embed {
        title = doc.docTitleElement.targetElement.text()
        url = doc.onlineURL

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
                doc.onlineURL
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
                doc.onlineURL
            )
        }

        builder.addDocDetails(doc, includedTypes)

        builder.addSeeAlso(doc.seeAlso, doc.onlineURL)
    }

    fun toEmbed(clazz: JavadocClass, method: JavadocMethod): MessageEmbed = embed {
        var title = method.getSimpleAnnotatedSignature(clazz)
        if (title.length > MessageEmbed.TITLE_MAX_LENGTH) {
            title = "%s%s#%s : %s - [full signature on online docs]".format(
                if (method.isStatic) "static " else "",
                method.declaringClass.className,
                method.methodName,
                method.methodReturnType
            )
        }

        builder.setTitle(title, method.onlineURL)

        //Should use that but JB annotations are duplicated, bruh momentum
//		    builder.setTitle(method.getMethodSignature(), method.getURL());
        if (clazz != method.declaringClass) {
            builder.setDescription("**Inherited from ${method.declaringClass.className}**\n\n")
        }

        builder.addDocDescription(method)
        builder.addDocDeprecation(method)
        builder.addDocDetails(method, includedTypes)
        builder.addSeeAlso(method.seeAlso, method.onlineURL)
    }

    fun toEmbed(clazz: JavadocClass, field: JavadocField): MessageEmbed = embed {
        title = clazz.className + " : " + field.simpleSignature
        url = field.onlineURL

        if (clazz != field.declaringClass) {
            description = "**Inherited from ${field.declaringClass.className}**\n\n"
        }

        builder.addDocDescription(field)
        builder.addDocDeprecation(field)

        field.fieldValue?.let { fieldValue ->
            field {
                name = "Value"
                value = fieldValue
            }
        }

        builder.addDocDetails(field, includedTypes)
        builder.addSeeAlso(field.seeAlso, field.onlineURL)
    }

    private inline fun embed(block: InlineEmbed.() -> Unit): MessageEmbed {
        return EmbedBuilder().let(::InlineEmbed).apply(block).build()
    }

    private fun EmbedBuilder.addDocDescription(doc: AbstractJavadoc) {
        val descriptionElement = doc.descriptionElements
        if (descriptionElement.isNotEmpty()) {
            appendDescription(
                getDescriptionValue(
                    descriptionBuilder.length,
                    descriptionElement.toMarkdown("\n\n"), //Description blocks are separated like paragraphs, unlike details such as "Specified by"
                    doc.onlineURL
                )
            )
        } else {
            appendDescription("No description")
        }
    }

    private fun EmbedBuilder.addDocDeprecation(doc: AbstractJavadoc) {
        val deprecationElement = doc.deprecationElement
        if (deprecationElement != null) {
            this.addDocField(
                "Deprecated",
                deprecationElement.toMarkdown(),
                false,
                doc.onlineURL
            )
        }
    }

    private fun EmbedBuilder.addDocDetails(doc: AbstractJavadoc, excludedTypes: EnumSet<DocDetail.Type>) {
        val details = doc.getDetails(excludedTypes)
        for (detail in details) {
            addDocField(
                detail.detailString,
                detail.toMarkdown("\n"),
                false,
                doc.onlineURL
            )
        }
    }

    private fun getDescriptionValue(currentLength: Int, descriptionValue: String, onlineTarget: String?): String {
        return when {
            descriptionValue.length + currentLength > DESCRIPTION_MAX_LENGTH -> {
                when (onlineTarget) {
                    null -> "Description is too long. Please look at the docs in your IDE"
                    else -> "Description is too long. Please look at [the online docs]($onlineTarget)"
                }
            }

            else -> descriptionValue
        }
    }

    private fun EmbedBuilder.addDocField(
        fieldName: String,
        fieldValue: String,
        inline: Boolean,
        onlineDocs: String?
    ) {
        if (fieldValue.length > MessageEmbed.VALUE_MAX_LENGTH) {
            val replacement = when {
                onlineDocs != null -> "This section is too long. Please look at [the online docs]($onlineDocs)"
                else -> "This section is too long. Please look at the docs in your IDE"
            }

            addField(fieldName, replacement, inline)
        } else {
            addField(fieldName, fieldValue, inline)
        }
    }

    private fun EmbedBuilder.addSeeAlso(seeAlso: SeeAlso?, onlineDocs: String?) {
        if (seeAlso == null) return

        val seeAlsoMd = seeAlso.references.joinToString(", ") { ref: SeeAlsoReference ->
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