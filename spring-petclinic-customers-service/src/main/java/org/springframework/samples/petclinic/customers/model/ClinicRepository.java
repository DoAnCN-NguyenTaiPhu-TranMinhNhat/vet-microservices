package org.springframework.samples.petclinic.customers.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
}
