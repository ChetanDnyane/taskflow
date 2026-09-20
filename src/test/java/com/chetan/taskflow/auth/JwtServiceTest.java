package com.chetan.taskflow.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void generatedTokenIsSignedContainsEmailAndExpiresOneHourAfterIssue() {
        var service = new JwtService(TEST_KEY);
        Instant before = Instant.now().minusSeconds(1);

        String token = service.generateToken("alice@example.com");

        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_KEY)))
                .build().parseSignedClaims(token).getPayload();
        assertThat(service.extractEmail(token)).isEqualTo("alice@example.com");
        assertThat(claims.getIssuedAt().toInstant()).isBetween(before, Instant.now());
        assertThat(claims.getExpiration().toInstant())
                .isEqualTo(claims.getIssuedAt().toInstant().plusSeconds(3600));
        assertThat(claims).doesNotContainKeys("password", "passwordHash");
    }
}
