/*
 * Copyright 2016 Marco Lombardo
 * https://github.com/mar9000
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mar9000.space2latex.latex;

public class TextElement implements LatexElement {
	
	public String text = null;
	
	public TextElement(String text) {
		this.text = identifyLongPackage(escapeString(text));
	}
	
	public static String escapeString(String text) {
		StringBuffer result = new StringBuffer();
		for (int c = 0; c < text.length(); c++) {
			char current = text.charAt(c);
			switch (current) {
			case '{':
				result.append("\\{");
				break;
			case '}':
				result.append("\\}");
				break;
			case '\\':
				result.append("\\localBackslash{}");
				break;
			case '\u00A0':
				result.append(" ");   // Replace &nbsp; with space.
				break;
			case '\u201C':
				result.append("``");
				break;
			case '\u201D':
				result.append("''");
				break;
			case '\u2019':
				result.append("'");
				break;
			case '\u2192':   // Rightwards arrow.
				result.append("$\\rightarrow$");
				break;
			case '\u2014':   // &mdash;, an alternative is /textemdash .
				result.append("---");
				break;
			case '\u2013':      // &ndash;, an alternative is /textendash .
				result.append("--");
				break;
			case '\uFEFF':   // ZERO WIDTH NO-BREAK.
				result.append("\\hspace{0pt}");
				break;
			default:
				if ("#$%&~_^".indexOf(current) != -1)
					result.append("\\").append(current);
				else
					result.append(current);
				break;
			}
		}
		/*
		// Can't use $\backslash$ or "$" will be escaped.
		String newText = text
				.replace("{", "\\{")
				.replace("}", "\\}")
				.replace("\\", "\\localBackslash{}")
				.replaceAll("([#$%&~_^])", "\\\\$1")
				.replace('\u00a0', ' ')   // Replace &nbsp; with space.
				.replace("\u201C", "``")
				.replace("\u201D", "''")
				.replace("\u2019", "'")
				.replace("\u00A0", " ")   // No break space.
				.replace("\u2192", "$\\rightarrow$")   // Rightwards arrow.
				.replace("\u2014", "---")   // &mdash;, an alternative is /textemdash .
				.replace("\u2013", "--")   // &ndash;, an alternative is /textendash .
				.replace("\uFEFF", "\\hspace{0pt}")   // ZERO WIDTH NO-BREAK.
				;
		*/
		return result.toString();
	}

	public static final String JAVA_IDENTIFIER = "[a-zA-Z$_][a-zA-Z0-9$_]*";
	public static final String JAVA_PACKAGE_TWO_DOT = JAVA_IDENTIFIER + "\\." + JAVA_IDENTIFIER + "\\." + JAVA_IDENTIFIER;
	public static final String JAVA_PACKAGE_MORE_DOT = JAVA_PACKAGE_TWO_DOT + "(\\." + JAVA_IDENTIFIER + ")*";
	public static final String JAVA_PACKAGE_GROUP = "(" + JAVA_PACKAGE_MORE_DOT + ")";
	/**
	 * The latex template load the url package and define an url command "noclickurl", then the hyperref package is loaded.
	 */
	public static String identifyLongPackage(String text) {
		String newText = text.replaceAll(JAVA_PACKAGE_GROUP, "\\\\protect\\\\noclickurl{$1}");
		return newText;
	}
	
}
