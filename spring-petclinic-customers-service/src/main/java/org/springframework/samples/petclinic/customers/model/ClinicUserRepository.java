package org.springframework.samples.petclinic.customers.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClinicUserRepository extends JpaRepository<ClinicUser, UUID> {

	Optional<ClinicUser> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}
