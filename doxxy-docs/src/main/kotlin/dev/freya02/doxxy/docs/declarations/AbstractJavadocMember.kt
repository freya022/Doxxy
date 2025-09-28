package dev.freya02.doxxy.docs.declarations

abstract class AbstractJavadocMember internal constructor() : AbstractJavadoc() {
    /** methodName(Type1, Type2) / fieldName */
    abstract val identifier: String?
    abstract val returnType: String?
}
