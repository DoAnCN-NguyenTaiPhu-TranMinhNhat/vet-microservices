package org.springframework.samples.petclinic.vets.web;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Parses the same HS256 clinic JWT as customers-service to read {@code veterinarianId} and
 * {@code clinicAdmin} (no Spring Security filter — optional header on API calls from the gateway / SPA).
 */
@Component
class VetJwtSupport {

    private static final String DEFAULT_HS256_SECRET =
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final SecretKey signingKey;

    VetJwtSupport(
        @Value("${spring.security.oauth2.resourceserver.jwt.secret-key:}") String rawSecret) {
        String effective = StringUtils.hasText(rawSecret) ? rawSecret.trim() : DEFAULT_HS256_SECRET;
        byte[] keyBytes = effective.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes; check JWT_SECRET / oauth2 jwt secret-key");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    Optional<UUID> readVeterinarianId(String authorizationHeader) {
        Optional<Claims> claims = parseClaims(authorizationHeader);
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        return parseUuid(claims.get().get("veterinarianId"));
    }

    Optional<UUID> readClinicId(String authorizationHeader) {
        Optional<Claims> claims = parseClaims(authorizationHeader);
        if (claims.isEmpty()) {
            return Optional.empty();
        }
        return parseUuid(claims.get().get("clinicId"));
    }

    boolean readClinicAdmin(String authorizationHeader) {
        Optional<Claims> claims = parseClaims(authorizationHeader);
        if (claims.isEmpty()) {
            return false;
        }
        Object raw = claims.get().get("clinicAdmin");
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw != null) {
            return Boolean.parseBoolean(raw.toString());
        }
        return false;
    }

    private Optional<Claims> parseClaims(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7).trim();
        try {
            return Optional.of(Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<UUID> parseUuid(Object raw) {
        if (raw == null) {
            return Optional.empty();
        }
        if (raw instanceof UUID u) {
            return Optional.of(u);
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
