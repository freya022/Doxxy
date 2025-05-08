package dev.freya02.doxxy.docs.exceptions

class DocParseException : IllegalArgumentException {
    internal constructor()
    internal constructor(s: String) : super(s)
}