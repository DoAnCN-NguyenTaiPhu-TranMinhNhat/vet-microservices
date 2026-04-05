package org.springframework.samples.petclinic.customers.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.samples.petclinic.customers.model.ClinicUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtTokenService {

	/** Same default as application.yml; used when JWT_SECRET is unset or blank in the environment. */
	private static final String DEFAULT_HS256_SECRET =
		"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private final SecretKey signingKey;
	private final long expirationMs;

	public JwtTokenService(
		@Value("${spring.security.oauth2.resourceserver.jwt.secret-key:}") String rawSecret,
		@Value("${vet.clinic-auth.jwt-expiration-ms:86400000}") long expirationMs) {
		String effective = StringUtils.hasText(rawSecret) ? rawSecret.trim() : DEFAULT_HS256_SECRET;
		byte[] keyBytes = effective.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException(
				"JWT secret-key must be at least 256 bits (32 UTF-8 bytes); check JWT_SECRET in Docker / .env");
		}
		this.signingKey = Keys.hmacShaKeyFor(keyBytes);
		this.expirationMs = expirationMs;
	}

	public String createToken(ClinicUser user) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + expirationMs);
		var builder = Jwts.builder()
			.subject(user.getEmail())
			.claim("userId", user.getId().toString())
			.claim("clinicId", user.getClinic().getId().toString())
			.claim("clinicName", user.getClinic().getName())
			.claim("name", user.getDisplayName())
			.claim("clinicAdmin", user.isClinicAdmin())
			.issuedAt(now)
			.expiration(exp)
			.signWith(signingKey, Jwts.SIG.HS256);
		if (user.getVeterinarianId() != null) {
			builder.claim("veterinarianId", user.getVeterinarianId().toString());
		}
		return builder.compact();
	}

	public long getExpirationMs() {
		return expirationMs;
	}

	/**
	 * Reads {@code clinicId} from a Bearer token issued by this service (same signing key).
	 */
	public Optional<UUID> readClinicId(String authorizationHeader) {
		return readClaimUuid(authorizationHeader, "clinicId");
	}

	/**
	 * Reads {@code userId} from a Bearer token issued by this service.
	 */
	public Optional<UUID> readUserId(String authorizationHeader) {
		return readClaimUuid(authorizationHeader, "userId");
	}

	private Optional<UUID> readClaimUuid(String authorizationHeader, String claimName) {
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
			return Optional.empty();
		}
		String token = authorizationHeader.substring(7).trim();
		try {
			Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
			return parseUuid(claims.get(claimName));
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
