package com.matriz.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests of the task title rule: 3 to 150 characters after trimming, not only spaces
 * (FR-04, US-04, EC-03, docs/openapi.yaml CreateTaskRequest and UpdateTaskRequest; Practical
 * Worksheet 04 Exercise 2). Each case is checked for creating and for editing a task. Bean
 * Validation only: no Spring context and no database.
 */
class CreateTaskRequestTests {

	static ValidatorFactory factory;

	static Validator validator;

	@BeforeAll
	static void createValidator() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		factory.close();
	}

	@Test
	void acceptsAValidTitle() {
		assertThat(titleErrors("Write the copy")).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(ints = { 3, 150 })
	void acceptsTitlesAtTheLimits(int length) {
		assertThat(titleErrors("x".repeat(length))).isEmpty();
	}

	@Test
	void ignoresLeadingAndTrailingSpacesWhenCountingTheLength() {
		assertThat(titleErrors("  abc  ")).isEmpty();
		assertThat(titleErrors(" " + "x".repeat(150) + " ")).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(ints = { 2, 151 })
	void rejectsTitlesJustOutsideTheLimits(int length) {
		assertThat(titleErrors("x".repeat(length))).containsExactly("Title must be between 3 and 150 characters.");
	}

	@Test
	void countsCharactersNotUtf16UnitsSoEmojiCountOnce() {
		String emoji = "😀"; // one character, two UTF-16 units
		assertThat(titleErrors(emoji.repeat(150))).isEmpty();
		assertThat(titleErrors(emoji.repeat(151))).containsExactly("Title must be between 3 and 150 characters.");
		assertThat(titleErrors(emoji.repeat(2))).containsExactly("Title must be between 3 and 150 characters.");
	}

	@Test
	void rejectsATitleThatIsTooShortOnceTrimmed() {
		assertThat(titleErrors("  ab  ")).containsExactly("Title must be between 3 and 150 characters.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "     " })
	void rejectsAnEmptyTitleOrOneMadeOnlyOfSpaces(String title) {
		assertThat(titleErrors(title)).contains("Title must not be empty.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "\u00A0\u00A0\u00A0", "\uFEFF\u3000\u2003", "\t\n\r " })
	void treatsAnyUnicodeSpaceAsASpace(String spaces) {
		// No-break, zero-width no-break, ideographic and em spaces, tab and line breaks, like
		// the form (JavaScript trim()).
		assertThat(titleErrors(spaces)).contains("Title must not be empty.");
		assertThat(titleErrors(spaces + "ab" + spaces)).containsExactly("Title must be between 3 and 150 characters.");
		assertThat(titleErrors(spaces + "abc" + spaces)).isEmpty();
	}

	@Test
	void rejectsAMissingTitle() {
		assertThat(titleErrors(null)).containsExactly("Title must not be empty.");
	}

	/** The title errors of a new task, after checking that editing a task gives the same ones. */
	private static Set<String> titleErrors(String title) {
		Set<String> create = titleErrorsOf(new CreateTaskRequest(title, null, null, null));
		Set<String> edit = titleErrorsOf(new UpdateTaskRequest(title, null, null, Priority.MEDIUM));
		assertThat(edit).as("editing a task uses the same title rule as creating one").isEqualTo(create);
		return create;
	}

	private static Set<String> titleErrorsOf(Object request) {
		return validator.validate(request)
			.stream()
			.filter(violation -> violation.getPropertyPath().toString().equals("title"))
			.map(ConstraintViolation::getMessage)
			.collect(Collectors.toSet());
	}

}
