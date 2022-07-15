/*
 *     Copyright 2016-2017 Michael Ritter (Kantenkugel)
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

package com.freya02.bot.utils;

import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;

import java.util.regex.Pattern;

public class JDocUtil {
	private static final Pattern FIX_NEW_LINES_PATTERN = Pattern.compile("\n{3,}");
	private static final Pattern FIX_SPACE_PATTERN = Pattern.compile("\\h");

	private static final Remark REMARK;

	static {
		Options remarkOptions = Options.github();
		remarkOptions.inlineLinks = true;
		remarkOptions.fencedCodeBlocksWidth = 3;
		REMARK = new Remark(remarkOptions);
	}

	public static String formatText(String docs, String currentUrl) {
		String markdown = REMARK.convertFragment(fixSpaces(docs), currentUrl);

		//remove unnecessary carriage return chars
		markdown = FIX_NEW_LINES_PATTERN.matcher(
						markdown.replace("\r", "")
								//fix codeblocks
								.replace("\n\n```", "\n\n```java")
				)
				//remove too many newlines (max 2)
				.replaceAll("\n\n");

		return markdown;
	}

	static String fixSpaces(String input) {
		return FIX_SPACE_PATTERN.matcher(input).replaceAll(" ");
	}
}
