package com.issuetracker.auth;

import java.time.Duration;

public record IssuedTokens(String accessToken, String refreshToken, Duration accessTokenTtl) {
}
