package com.issuetracker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Same response for an unknown email and a wrong password, so callers cannot tell which accounts exist.
 */
public class InvalidCredentialsException extends ErrorResponseException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED,
            ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password."), null);
    }
}
