package org.springframework.samples.petclinic.customers.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OwnerRequest(@NotBlank String firstName,
                           @NotBlank String lastName,
                           @NotBlank String address,
                           @NotBlank String city,
                           @NotBlank
                           @Size(max = 20)
                           @Pattern(regexp = "^[+0-9][0-9 ]{3,19}$")
                           String telephone,
                           UUID clinicId
) {
}
