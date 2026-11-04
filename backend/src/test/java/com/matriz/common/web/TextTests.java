package com.matriz.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Spaces are the same as in the frontend: exactly the 25 characters JavaScript's
 * {@code trim()} removes (checked against Node.js for every UTF-16 code unit; EC-03, EC-10).
 */
class TextTests {

	@ParameterizedTest
	@ValueSource(ints = { 0x9, 0xA, 0xB, 0xC, 0xD, 0x20, 0xA0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF })
	void trimsEveryCharacterThatJavaScriptTrims(int space) {
		String s = String.valueOf((char) space);
		assertThat(Text.trim(s + "abc" + s)).isEqualTo("abc");
		assertThat(Text.isBlank(s + s)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(ints = { 0x1C, 0x1D, 0x1E, 0x1F, 0x85, 0x200B })
	void keepsLookAlikesThatJavaScriptKeeps(int character) {
		// U+001C to U+001F are removed by Java's strip(); U+0085 and U+200B by neither.
		String s = String.valueOf((char) character);
		assertThat(Text.trim(s + "abc" + s)).isEqualTo(s + "abc" + s);
		assertThat(Text.isBlank(s)).isFalse();
	}

	@Test
	void keepsSpacesInsideTheText() {
		assertThat(Text.trim("\u00A0 Plan the\u00A0beta \u3000")).isEqualTo("Plan the\u00A0beta");
		assertThat(Text.isBlank(null)).isTrue();
		assertThat(Text.isBlank("")).isTrue();
	}

}
