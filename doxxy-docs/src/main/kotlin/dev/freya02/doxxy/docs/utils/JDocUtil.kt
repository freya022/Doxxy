/*
 *     Copyright 2016-2017 Michael Ritter (Kantenkugel), freya022
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package dev.freya02.doxxy.docs.utils

import com.overzealous.remark.Options
import com.overzealous.remark.Remark
import java.util.regex.Pattern

internal object JDocUtil {
    private val linkEscapeRegex = Regex("""<\[(.*?)]\((.*)\)>""")
    private val FIX_NEW_LINES_PATTERN = Pattern.compile("\n{3,}")
    private val FIX_SPACE_PATTERN = Pattern.compile("\\h")
    private val REMARK: Remark =
        Options.github().apply {
            inlineLinks = true
            fencedCodeBlocksWidth = 3
        }.let { Remark(it) }

    fun formatText(docs: String, currentUrl: String): String {
        var markdown = REMARK.convertFragment(fixSpaces(docs), currentUrl)

        //remove unnecessary carriage return chars
        markdown = FIX_NEW_LINES_PATTERN.matcher(
            markdown.replace("\r", "") //fix codeblocks
                .replace("\n\n```", "\n\n```java")
        ).replaceAll("\n\n") //remove too many newlines (max 2)
        return markdown.escapeAutolink()
    }

    private fun fixSpaces(input: String): String {
        return FIX_SPACE_PATTERN.matcher(input).replaceAll(" ")
    }

    /**
     * Escapes links surrounded with <>, including markdown links
     */
    private fun String.escapeAutolink() = this.replace(linkEscapeRegex, """\\<[$1]($2)\\>""")
}