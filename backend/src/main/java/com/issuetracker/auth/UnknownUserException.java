package com.issuetracker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * The access token is valid but its user no longer exists.
 */
public class UnknownUserException extends ErrorResponseException {

    public UnknownUserException() {
        super(HttpStatus.UNAUTHORIZED,
            ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Your session is no longer valid."), null);
    }
}
