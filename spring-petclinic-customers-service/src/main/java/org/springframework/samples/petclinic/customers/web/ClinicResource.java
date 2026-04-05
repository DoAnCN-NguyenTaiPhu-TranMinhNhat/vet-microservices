package org.springframework.samples.petclinic.customers.web;

import io.micrometer.core.annotation.Timed;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.samples.petclinic.customers.model.Clinic;
import org.springframework.samples.petclinic.customers.model.ClinicRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only catalog of clinics for internal tools (e.g. vet-ai MLOps admin dropdown).
 */
@RequestMapping("/clinics")
@RestController
@Timed("petclinic.clinic")
public class ClinicResource {

	private final ClinicRepository clinicRepository;

	public ClinicResource(ClinicRepository clinicRepository) {
		this.clinicRepository = clinicRepository;
	}

	@GetMapping
	public List<ClinicDetails> list() {
		return clinicRepository.findAll().stream()
			.map(this::toDetails)
			.sorted(Comparator.comparing(ClinicDetails::id, Comparator.nullsLast(Comparator.naturalOrder())))
			.toList();
	}

	private ClinicDetails toDetails(Clinic c) {
		return new ClinicDetails(c.getId(), c.getName(), c.getPhone(), c.getAddress());
	}

	public record ClinicDetails(UUID id, String name, String phone, String address) {
	}
}
