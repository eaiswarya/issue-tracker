package com.issuetracker.auth;

/**
 * {@code expiresIn} is the access token lifetime in seconds.
 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

    public static TokenResponse from(IssuedTokens tokens) {
        return new TokenResponse(
            tokens.accessToken(), tokens.refreshToken(), "Bearer", tokens.accessTokenTtl().toSeconds());
    }
}
