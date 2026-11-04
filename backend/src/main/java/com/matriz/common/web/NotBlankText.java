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
 * The text is present and not made only of spaces, where a space is any Unicode space, like
 * in the frontend ({@link Text}). Use instead of {@code @NotBlank}, which only treats ASCII
 * spaces as spaces (EC-03, EC-10).
 */
@Documented
@Constraint(validatedBy = NotBlankText.Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBlankText {

	String message();

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<NotBlankText, CharSequence> {

		@Override
		public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
			return value != null && !Text.isBlank(value.toString());
		}

	}

}
