package com.freya02.bot.format

import com.palantir.javaformat.java.Formatter
import com.palantir.javaformat.java.FormatterException

object Formatter {
    private val classTemplate = FormatterTemplate(
        """
        public class Test {
            {}
        }
    """.trimIndent(),
        """
        public class Test \{
        (\X*)
        }
    """.trimIndent()
    )

    private val methodTemplate = FormatterTemplate(
        """
        public class Test {
            void test() {
                {}            
            }
        }
    """.trimIndent(),
        """
        public class Test \{
        \s*void test\(\) \{
        (\X*)
        \s*}
        }
    """.trimIndent()
    )

    private val templates = listOf(classTemplate, methodTemplate)

    //TODO use FormattingException
    fun format(userSource: String): String? {
        for (template in templates) {
            try {
                val formattedClass = Formatter
                    .create()
                    .formatSource(template.withSource(userSource))

                return template.extractRegex
                    .matchEntire(formattedClass.trimEnd())!! //Formatter adds a new line which breaks the regex
                    .groups[1]!!
                    .value
                    .trimIndent()
            } catch (_: FormatterException) {
            }
        }

        return null
    }
}