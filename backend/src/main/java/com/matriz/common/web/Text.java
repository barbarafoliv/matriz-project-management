package com.matriz.common.web;

/**
 * Trimming and blank text for names, titles, descriptions, comments and searches, the same as
 * the frontend (JavaScript {@code String.trim()}): a space is any Unicode space (such as a
 * no-break space), a tab, a line break or U+FEFF (docs/openapi.yaml, EC-03, EC-10).
 */
public final class Text {

	private Text() {
	}

	/** The text without leading and trailing spaces. */
	public static String trim(String text) {
		int start = 0;
		int end = text.length();
		while (start < end && isSpace(text.charAt(start))) {
			start++;
		}
		while (end > start && isSpace(text.charAt(end - 1))) {
			end--;
		}
		return text.substring(start, end);
	}

	/** True when the text is null, empty or made only of spaces. */
	public static boolean isBlank(String text) {
		return text == null || trim(text).isEmpty();
	}

	/**
	 * Unicode space separators, line and paragraph separators (all in {@code isSpaceChar}),
	 * tab, line feed, vertical tab, form feed, carriage return and U+FEFF: what
	 * JavaScript's {@code trim()} removes.
	 */
	static boolean isSpace(char c) {
		return Character.isSpaceChar(c) || (c >= '\t' && c <= '\r') || c == '\uFEFF';
	}

}
