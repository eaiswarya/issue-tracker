package com.issuetracker.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Only thrown after the password has been verified, so it reveals nothing to someone who does not know it.
 */
public class AccountDeactivatedException extends ErrorResponseException {

    public AccountDeactivatedException() {
        super(HttpStatus.FORBIDDEN,
            ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "This account has been deactivated."), null);
    }
}
