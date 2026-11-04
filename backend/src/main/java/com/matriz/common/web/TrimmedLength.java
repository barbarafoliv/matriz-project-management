package com.matriz.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * The text's length in characters, without leading and trailing spaces, is between
 * {@link #min()} and {@link #max()}. Names and titles are stored trimmed (docs/openapi.yaml).
 * Null is valid; use {@code @NotBlank} to require a value.
 */
@Documented
@Constraint(validatedBy = TrimmedLength.Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface TrimmedLength {

	int min() default 0;

	int max() default Integer.MAX_VALUE;

	String message();

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<TrimmedLength, CharSequence> {

		private int min;

		private int max;

		@Override
		public void initialize(TrimmedLength annotation) {
			this.min = annotation.min();
			this.max = annotation.max();
		}

		@Override
		public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
			if (value == null) {
				return true;
			}
			// Characters (code points), like the database and the contract: an emoji counts once.
			String trimmed = value.toString().strip();
			int length = trimmed.codePointCount(0, trimmed.length());
			return length >= this.min && length <= this.max;
		}

	}

}
