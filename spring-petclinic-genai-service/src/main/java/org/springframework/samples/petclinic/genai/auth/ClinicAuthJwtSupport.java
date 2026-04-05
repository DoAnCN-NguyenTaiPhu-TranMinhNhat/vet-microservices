package org.springframework.samples.petclinic.genai.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Reads {@code clinicId} from the same HS256 JWT issued by customers-service (Bearer token on diagnosis/feedback).
 * Used when the browser omits {@code clinicId} in the JSON body.
 */
@Component
public class ClinicAuthJwtSupport {

	private static final String DEFAULT_HS256_SECRET =
		"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private final SecretKey signingKey;

	public ClinicAuthJwtSupport(
		@Value("${spring.security.oauth2.resourceserver.jwt.secret-key:}") String rawSecret) {
		String effective = StringUtils.hasText(rawSecret) ? rawSecret.trim() : DEFAULT_HS256_SECRET;
		byte[] keyBytes = effective.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException(
				"JWT secret-key must be at least 256 bits (32 UTF-8 bytes); set JWT_SECRET or spring.security.oauth2.resourceserver.jwt.secret-key");
		}
		this.signingKey = Keys.hmacShaKeyFor(keyBytes);
	}

	public Optional<UUID> readClinicId(String authorizationHeader) {
		if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
			return Optional.empty();
		}
		String token = authorizationHeader.substring(7).trim();
		try {
			Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
			Object raw = claims.get("clinicId");
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
			return Optional.of(UUID.fromString(s));
		}
		catch (Exception ignored) {
			return Optional.empty();
		}
	}
}
