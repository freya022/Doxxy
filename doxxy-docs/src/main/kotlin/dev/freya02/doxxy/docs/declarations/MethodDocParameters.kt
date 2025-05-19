package dev.freya02.doxxy.docs.declarations

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments
import dev.freya02.doxxy.docs.utils.DecomposedName
import org.jsoup.nodes.Element

@ConsistentCopyVisibility
data class MethodDocParameters internal constructor(
    val originalText: String,
    val parameters: List<MethodDocParameter>
)

internal fun MethodDocParameters(element: Element): MethodDocParameters {
    require(element.className() == "parameters") { "Node with the 'parameters' class should be passed" }

    val parameters = StaticJavaParser.parseMethodDeclaration("void x${element.text()};")
        .parameters
        .onEach {
            val type = it.type
            if (type is NodeWithTypeArguments<*>) type.removeTypeArguments()
        }
    val linkedTypes = element.select("a").filterTo(arrayListOf()) { it.text().none { c -> c == '@' } }

    // On each parameter we look up the node which's effective text is the param type,
    // and get the link to get the actual package (!!! must do it in the correct order as there could be two types with diff packages in theory)
    // If there's no link, then assume FQCN
    return MethodDocParameters(
        element.text(),
        parameters.map { parameter ->
            try {
                val fullType = run {
                    val linkedType = linkedTypes.getOrNull(linkedTypes.indexOfFirst { it.text() == parameter.typeAsString })
                        ?: return@run parameter.typeAsString

                    val (packageName, className) = DecomposedName.getDecompositionFromLink(linkedType)
                    requireNotNull(packageName) { "Package is null for $linkedType"}
                    "$packageName.$className"
                }

                val simpleType = run {
                    val index = parameter.typeAsString.indexOfFirst { it.isUpperCase() }.coerceAtLeast(0)
                    parameter.typeAsString.drop(index)
                }

                MethodDocParameter(
                    annotations = parameter.annotations.mapTo(linkedSetOf()) { it.nameAsString },
                    type = fullType + if (parameter.isVarArgs) "..." else "",
                    simpleType = simpleType + if (parameter.isVarArgs) "..." else "",
                    name = parameter.nameAsString,
                )
            } catch (e: Exception) {
                throw RuntimeException("Unable to parse parameter '$parameter'", e)
            }
        }
    )
}
