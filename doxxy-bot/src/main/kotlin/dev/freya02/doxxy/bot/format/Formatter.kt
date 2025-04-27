package dev.freya02.doxxy.bot.format

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

    private val noTemplate = FormatterTemplate(
        """
        {}
    """.trimIndent(),
        """
        (\X*)
    """.trimIndent()
    )

    private val noTemplateMissingCurlyBracket = FormatterTemplate(
        """
        {}}
    """.trimIndent(),
        """
        (\X*)
    """.trimIndent()
    )

    private val templates = listOf(classTemplate, methodTemplate, noTemplate, noTemplateMissingCurlyBracket)

    @Throws(FormattingException::class)
    fun format(userSource: String): String {
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

        throw FormattingException()
    }
}