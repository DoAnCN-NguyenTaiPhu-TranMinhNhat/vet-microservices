package org.springframework.samples.petclinic.customers.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "clinic_users")
public class ClinicUser {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(columnDefinition = "VARCHAR(36)", length = 36)
	private UUID id;

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "clinic_id", nullable = false)
	private Clinic clinic;

	@NotBlank
	@Email
	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 120)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "veterinarian_id", columnDefinition = "VARCHAR(36)", length = 36)
	private UUID veterinarianId;

	/** When true, JWT includes clinicAdmin; vets-service allows full vet CRUD (not single-vet scoped). */
	@Column(name = "clinic_admin", nullable = false)
	private boolean clinicAdmin;

	public UUID getId() {
		return id;
	}

	public Clinic getClinic() {
		return clinic;
	}

	public void setClinic(Clinic clinic) {
		this.clinic = clinic;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public UUID getVeterinarianId() {
		return veterinarianId;
	}

	public void setVeterinarianId(UUID veterinarianId) {
		this.veterinarianId = veterinarianId;
	}

	public boolean isClinicAdmin() {
		return clinicAdmin;
	}

	public void setClinicAdmin(boolean clinicAdmin) {
		this.clinicAdmin = clinicAdmin;
	}
}
