package org.springframework.samples.petclinic.customers.auth;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Timed("petclinic.clinic-auth")
public class ClinicAuthController {

	private final ClinicAuthService clinicAuthService;

	public ClinicAuthController(ClinicAuthService clinicAuthService) {
		this.clinicAuthService = clinicAuthService;
	}

	@PostMapping("/register")
	public ClinicAuthDtos.AuthResponse register(@Valid @RequestBody ClinicAuthDtos.RegisterRequest body) {
		return clinicAuthService.register(body);
	}

	@PostMapping("/login")
	public ClinicAuthDtos.AuthResponse login(@Valid @RequestBody ClinicAuthDtos.LoginRequest body) {
		return clinicAuthService.login(body);
	}

	@GetMapping("/me")
	public ClinicAuthDtos.UserInfo me(@RequestHeader(value = "Authorization", required = false) String authorization) {
		return clinicAuthService.me(authorization);
	}
}
