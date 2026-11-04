package com.matriz.common.web;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into the problem responses described in docs/openapi.yaml.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private static final List<String> FIELD_ERROR_ORDER = List.of("REQUIRED", "BLANK", "TOO_SHORT", "TOO_LONG",
			"INVALID_FORMAT", "INVALID_VALUE");

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
		return ResponseEntity.status(ex.getStatus())
			.headers(ex.getHeaders())
			.body(Problems.of(ex.getStatus(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(FieldValidationException.class)
	ResponseEntity<ProblemDetail> handleFieldValidation(FieldValidationException ex, HttpServletRequest request) {
		ProblemDetail problem = validationProblem(request.getRequestURI(),
				List.of(Map.of("field", ex.getField(), "code", ex.getFieldCode(), "message", ex.getMessage())));
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		return ResponseEntity.badRequest()
			.body(Problems.of(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, "The request is malformed.",
					request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.internalServerError()
			.body(Problems.of(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
					"Something went wrong. Try again later.", request.getRequestURI()));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest()
			.body(validationProblem(path(request), fieldErrors(ex.getBindingResult().getFieldErrors())));
	}

	/**
	 * Invalid path or query parameters, such as a project ID below 1, or an invalid body on
	 * a method that also validates its path parameters.
	 */
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> bodyErrors = ex.getBeanResults()
			.stream()
			.flatMap(result -> result.getFieldErrors().stream())
			.toList();
		if (!bodyErrors.isEmpty() && ex.getValueResults().isEmpty()) {
			return ResponseEntity.badRequest().body(validationProblem(path(request), fieldErrors(bodyErrors)));
		}
		return ResponseEntity.badRequest()
			.body(Problems.of(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, "The request is malformed.",
					path(request)));
	}

	/**
	 * One entry per field (EC-03): the most basic problem first, e.g. BLANK before INVALID_FORMAT.
	 */
	private static List<Map<String, String>> fieldErrors(List<FieldError> all) {
		Map<String, FieldError> firstPerField = new LinkedHashMap<>();
		all.stream()
			.sorted(Comparator.comparingInt(error -> FIELD_ERROR_ORDER.indexOf(fieldErrorCode(error))))
			.forEach(error -> firstPerField.putIfAbsent(error.getField(), error));
		return firstPerField.values()
			.stream()
			.map(error -> Map.of("field", error.getField(), "code", fieldErrorCode(error), "message",
					String.valueOf(error.getDefaultMessage())))
			.toList();
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest()
			.body(Problems.of(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, "The request is malformed.",
					path(request)));
	}

	/**
	 * Maps a Bean Validation failure to the contract's field error codes.
	 */
	private static String fieldErrorCode(FieldError error) {
		Object value = error.getRejectedValue();
		return switch (String.valueOf(error.getCode())) {
			case "NotNull", "NotEmpty" -> "REQUIRED";
			case "NotBlank", "NotBlankText" -> (value == null || value.toString().isEmpty()) ? "REQUIRED" : "BLANK";
			case "Size", "TrimmedLength", "CodePointLength" -> isTooLong(error) ? "TOO_LONG" : "TOO_SHORT";
			case "Email", "Pattern" -> "INVALID_FORMAT";
			default -> "INVALID_VALUE";
		};
	}

	private static boolean isTooLong(FieldError error) {
		if (error.contains(ConstraintViolation.class) && error.getRejectedValue() instanceof CharSequence value) {
			Object max = error.unwrap(ConstraintViolation.class).getConstraintDescriptor().getAttributes().get("max");
			// Counted the same way as the constraint that failed.
			int length = switch (String.valueOf(error.getCode())) {
				case "TrimmedLength" -> TrimmedLength.Validator.lengthOf(value);
				case "CodePointLength" -> value.toString().codePointCount(0, value.length());
				default -> value.length();
			};
			return max instanceof Integer limit && length > limit;
		}
		return false;
	}

	private static ProblemDetail validationProblem(String instance, List<Map<String, String>> errors) {
		ProblemDetail problem = Problems.of(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
				"One or more fields are invalid.", instance);
		problem.setTitle("Validation failed");
		problem.setProperty("errors", errors);
		return problem;
	}

	private static String path(WebRequest request) {
		return request.getDescription(false).replaceFirst("^uri=", "");
	}

}
