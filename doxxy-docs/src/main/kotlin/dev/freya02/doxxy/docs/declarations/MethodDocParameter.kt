package dev.freya02.doxxy.docs.declarations

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments

@ConsistentCopyVisibility
data class MethodDocParameter private constructor(
    val annotations: Set<String>,
    val type: String,
    val simpleType: String,
    val name: String
) {

    internal companion object {

        private val parser = JavaParser()

        internal fun parse(source: String): MethodDocParameter {
            val parsedParameter = synchronized(parser) {
                parser.parseParameter(source).result.get()
            }.also {
                val type = it.type
                if (type is NodeWithTypeArguments<*>) type.removeTypeArguments()
            }

            val typeAsString = parsedParameter.typeAsString

            val annotations = parsedParameter.annotations.map { it.nameAsString }.toSet()
            val type = typeAsString + if (parsedParameter.isVarArgs) "..." else ""
            val simpleType = run {
                val startIndex = typeAsString.indexOfFirst { it.isUpperCase() }.coerceAtLeast(0)
                typeAsString.substring(startIndex) + if (parsedParameter.isVarArgs) "..." else ""
            }
            val name = parsedParameter.nameAsString

            return MethodDocParameter(annotations, type, simpleType, name)
        }
    }
}
