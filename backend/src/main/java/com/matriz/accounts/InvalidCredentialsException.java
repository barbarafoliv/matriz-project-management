package com.matriz.accounts;

import org.springframework.http.HttpStatus;

import com.matriz.common.web.ApiException;
import com.matriz.common.web.ErrorCode;

/**
 * Wrong email or password. The same message is used for both (EC-02).
 */
class InvalidCredentialsException extends ApiException {

	InvalidCredentialsException() {
		super(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, "Email or password is incorrect.");
	}

}
