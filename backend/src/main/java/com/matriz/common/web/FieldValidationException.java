package com.matriz.common.web;

import org.springframework.http.HttpStatus;

/**
 * A field is invalid for a reason only the application can check, such as an email with no
 * account. Returned as {@code VALIDATION_FAILED} with one field error.
 */
public class FieldValidationException extends ApiException {

	private final String field;

	private final String fieldCode;

	public FieldValidationException(String field, String fieldCode, String message) {
		super(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message);
		this.field = field;
		this.fieldCode = fieldCode;
	}

	public String getField() {
		return this.field;
	}

	public String getFieldCode() {
		return this.fieldCode;
	}

}
