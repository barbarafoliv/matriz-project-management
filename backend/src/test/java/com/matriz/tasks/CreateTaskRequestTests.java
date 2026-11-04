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
 * (FR-04, US-04, EC-03, docs/openapi.yaml CreateTaskRequest; Practical Worksheet 04
 * Exercise 2). Bean Validation only: no Spring context and no database.
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
	void rejectsATitleThatIsTooShortOnceTrimmed() {
		assertThat(titleErrors("  ab  ")).containsExactly("Title must be between 3 and 150 characters.");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "     " })
	void rejectsAnEmptyTitleOrOneMadeOnlyOfSpaces(String title) {
		assertThat(titleErrors(title)).contains("Title must not be empty.");
	}

	@Test
	void rejectsAMissingTitle() {
		assertThat(titleErrors(null)).containsExactly("Title must not be empty.");
	}

	private static Set<String> titleErrors(String title) {
		Set<ConstraintViolation<CreateTaskRequest>> violations = validator
			.validate(new CreateTaskRequest(title, null, null, null));
		return violations.stream()
			.filter(violation -> violation.getPropertyPath().toString().equals("title"))
			.map(ConstraintViolation::getMessage)
			.collect(Collectors.toSet());
	}

}
