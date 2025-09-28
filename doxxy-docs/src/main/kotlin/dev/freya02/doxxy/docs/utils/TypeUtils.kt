package dev.freya02.doxxy.docs.utils

private val annotationRegex = Regex("@\\w*\\s*")

/**
 * Removes annotations from return types.
 *
 * Usually annotations are in a separate span,
 * but TYPE_USE annotations are on the (return) type itself, so we need to remove them.
 */
fun String.removeAnnotations(): String = annotationRegex.replace(this, "").trimStart()
