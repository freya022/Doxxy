package dev.freya02.doxxy.docs.data

enum class ClassDetailType(val detailId: String) {
    FIELD("field-detail"),
    CONSTRUCTOR("constructor-detail"),
    METHOD("method-detail"),
    ANNOTATION_ELEMENT("annotation-interface-element-detail"),
    ENUM_CONSTANTS("enum-constant-detail");
}