package com.issuetracker.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues JWT access tokens and opaque refresh tokens. Only a SHA-256 hash of each refresh token is stored.
 */
@Service
public class TokenService {

    static final String ROLE_CLAIM = "role";

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokens;
    private final AuthProperties properties;
    private final Clock clock;

    public TokenService(
            JwtEncoder jwtEncoder, RefreshTokenRepository refreshTokens, AuthProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokens issue(User user) {
        Instant now = clock.instant();
        String refreshToken = newRefreshToken();
        refreshTokens.save(new RefreshToken(user, hash(refreshToken), now.plus(properties.refreshTokenTtl()), now));
        return new IssuedTokens(accessToken(user, now), refreshToken, properties.accessTokenTtl());
    }

    /**
     * Exchanges a refresh token for a new pair; the old token can never be used again. Presenting a token that was
     * already revoked means it leaked or was replayed, so every refresh token of that user is revoked. Those
     * revocations are committed even though the method throws.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public IssuedTokens rotate(String refreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokens.findByTokenHashForUpdate(hash(refreshToken))
            .orElseThrow(InvalidRefreshTokenException::new);
        User user = current.getUser();
        if (current.isRevoked()) {
            refreshTokens.revokeAllActiveForUser(user.getId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpiredAt(now)) {
            throw new InvalidRefreshTokenException();
        }
        current.revoke(now);
        if (!user.isActive()) {
            throw new InvalidRefreshTokenException();
        }
        return issue(user);
    }

    /**
     * Logout. Unknown or already revoked tokens are ignored, so the response never reveals whether a token exists.
     */
    @Transactional
    public void revoke(String refreshToken) {
        refreshTokens.findByTokenHashForUpdate(hash(refreshToken)).ifPresent(token -> token.revoke(clock.instant()));
    }

    private String accessToken(User user, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(JwtConfig.ISSUER)
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(properties.accessTokenTtl()))
            .claim("email", user.getEmail())
            .claim(ROLE_CLAIM, user.getSystemRole().name())
            .build();
        JwsHeader header = JwsHeader.with(JwtConfig.ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
