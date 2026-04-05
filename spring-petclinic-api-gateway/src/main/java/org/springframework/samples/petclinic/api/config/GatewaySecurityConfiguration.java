package org.springframework.samples.petclinic.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

	/**
	 * Explicit bean so @WebFluxTest and all profiles get a decoder (slice tests may not register auto-config).
	 */
	@Bean
	public ReactiveJwtDecoder reactiveJwtDecoder(
		@Value("${spring.security.oauth2.resourceserver.jwt.secret-key:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef}") String rawSecret) {
		byte[] keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException(
				"spring.security.oauth2.resourceserver.jwt.secret-key must be at least 256 bits (32 UTF-8 bytes)");
		}
		var key = new SecretKeySpec(keyBytes, "HmacSHA256");
		return NimbusReactiveJwtDecoder.withSecretKey(key)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
	}

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		http
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.authorizeExchange(ex -> ex
				.pathMatchers("/api/customer/auth/**").permitAll()
				.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.pathMatchers(
					"/actuator/health",
					"/actuator/health/**",
					"/actuator/info",
					"/actuator/prometheus"
				).permitAll()
				.pathMatchers(
					"/",
					"/index.html",
					"/favicon.ico",
					"/webjars/**",
					"/css/**",
					"/images/**",
					"/scripts/**"
				).permitAll()
				.pathMatchers("/fallback", "/fallback/**").permitAll()
				.pathMatchers("/api/**").authenticated()
				.anyExchange().permitAll()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}
}
