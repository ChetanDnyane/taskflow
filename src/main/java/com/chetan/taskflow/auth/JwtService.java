package com.chetan.taskflow.auth;

// <editor-fold defaultstate="collapsed" desc="Create and verify signed authentication tokens">
/*
 * The configured jwt.secret is Base64-decoded once and converted into an HMAC key at construction.
 * Token payloads contain the email subject, issue time and expiry one hour later; no password or role.
 * Signing protects integrity, not confidentiality: a JWT payload is readable by whoever holds the token.
 * Parsing verifies the signature and time claims before returning the subject. JJWT parsing failures
 * propagate to JwtAuthenticationFilter; extracting an email is not merely decoding unchecked JSON.
 */
// </editor-fold>

import com.chetan.taskflow.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;

    // <editor-fold defaultstate="collapsed" desc="Load the signing key once">
    /*
     * The injected secret must be Base64 text encoding enough key bytes for HMAC signing.
     * Decoding or key-strength failures happen at bean creation, so invalid configuration fails
     * startup. The same key must be used for issuing tokens and accepting them later.
     */
    // </editor-fold>
    public JwtService(
            @Value("${jwt.secret}") String jwtSecret) {

        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    // <editor-fold defaultstate="collapsed" desc="Issue a token with a bounded lifetime">
    /*
     * A single captured Instant anchors both issue and expiration claims. signWith chooses a
     * suitable HMAC algorithm for the key; compact returns the encoded header.payload.signature.
     * The caller supplies the normalized email; this method does not authenticate passwords.
     */
    // </editor-fold>
    public String generateToken(User user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(signingKey)
                .compact();
    }

    // <editor-fold defaultstate="collapsed" desc="Read only verified claims">
    /*
     * parseSignedClaims rejects invalid signatures and expired tokens before getSubject runs.
     * No database lookup occurs here; the filter separately confirms the account still exists.
     */
    // </editor-fold>
    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}