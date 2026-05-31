package kr.devslab.kit.identity.core.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.AuthToken;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.UserStatus;

public class JjwtAuthTokenService implements AuthTokenService {

    private static final String CLAIM_TENANT = "tenant";
    private static final String CLAIM_PUBLIC_ID = "publicId";
    private static final String CLAIM_LOGIN_ID = "loginId";
    private static final String CLAIM_STATUS = "status";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_MUST_CHANGE_PASSWORD = "mustChangePassword";

    private final SecretKey signingKey;
    private final Duration ttl;
    private final String issuer;
    private final Clock clock;

    public JjwtAuthTokenService(String secret, Duration ttl, String issuer, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 bytes (256 bits) of UTF-8 for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl == null ? Duration.ofHours(8) : ttl;
        this.issuer = issuer == null ? "devslab-kit" : issuer;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public AuthToken issue(CurrentUser user) {
        Instant now = Instant.now(clock);
        Instant exp = now.plus(ttl);
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(user.id().value().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TENANT, user.tenantId().value())
                .claim(CLAIM_PUBLIC_ID, user.publicId().value())
                .claim(CLAIM_LOGIN_ID, user.loginId())
                .claim(CLAIM_STATUS, user.status().name())
                .claim(CLAIM_ROLES, user.roles())
                .claim(CLAIM_MUST_CHANGE_PASSWORD, user.mustChangePassword())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new AuthToken(token, now, exp);
    }

    @Override
    public Optional<CurrentUser> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            @SuppressWarnings("unchecked")
            Set<String> roles = Set.copyOf(((java.util.Collection<String>) claims.getOrDefault(CLAIM_ROLES, Set.of())));
            Boolean mustChangePassword = claims.get(CLAIM_MUST_CHANGE_PASSWORD, Boolean.class);
            CurrentUser user = new CurrentUser(
                    UserId.of(userId),
                    PublicId.of(claims.get(CLAIM_PUBLIC_ID, String.class)),
                    TenantId.of(claims.get(CLAIM_TENANT, String.class)),
                    claims.get(CLAIM_LOGIN_ID, String.class),
                    UserStatus.valueOf(claims.get(CLAIM_STATUS, String.class)),
                    roles,
                    mustChangePassword != null && mustChangePassword
            );
            return Optional.of(user);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
