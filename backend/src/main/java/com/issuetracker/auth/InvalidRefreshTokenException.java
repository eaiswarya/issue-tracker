package com.issuetracker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidRefreshTokenException extends ErrorResponseException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED,
            ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or expired."), null);
    }
}
