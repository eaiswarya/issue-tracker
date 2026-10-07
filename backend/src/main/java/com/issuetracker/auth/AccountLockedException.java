package com.issuetracker.auth;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class AccountLockedException extends ErrorResponseException {

    private final Duration retryAfter;

    public AccountLockedException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, problem(retryAfter), null);
        this.retryAfter = retryAfter;
        getHeaders().set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds(retryAfter)));
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    private static ProblemDetail problem(Duration retryAfter) {
        long minutes = Math.max(1, (retryAfterSeconds(retryAfter) + 59) / 60);
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
            "Too many failed login attempts. Try again in " + minutes + (minutes == 1 ? " minute." : " minutes."));
    }

    private static long retryAfterSeconds(Duration retryAfter) {
        // Round up so clients never retry before the lock has lifted.
        return retryAfter.toSeconds() + (retryAfter.toNanosPart() > 0 ? 1 : 0);
    }
}
