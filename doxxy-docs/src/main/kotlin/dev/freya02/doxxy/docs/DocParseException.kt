package dev.freya02.doxxy.docs

class DocParseException : IllegalArgumentException {
    internal constructor()
    internal constructor(s: String) : super(s)
}