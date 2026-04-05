package org.springframework.samples.petclinic.customers.web;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.config.SecurityConfiguration;
import org.springframework.samples.petclinic.customers.model.Clinic;
import org.springframework.samples.petclinic.customers.model.ClinicRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ClinicResource.class)
@Import(SecurityConfiguration.class)
@ActiveProfiles("test")
class ClinicResourceTest {

	@Autowired
	MockMvc mvc;

	@MockBean
	ClinicRepository clinicRepository;

	@Test
	void listClinicsReturnsSortedJson() throws Exception {
		UUID idA = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID idB = UUID.fromString("00000000-0000-0000-0000-000000000002");

		Clinic a = mock(Clinic.class);
		when(a.getId()).thenReturn(idB);
		when(a.getName()).thenReturn("Beta");
		when(a.getPhone()).thenReturn("1");
		when(a.getAddress()).thenReturn("A");

		Clinic b = mock(Clinic.class);
		when(b.getId()).thenReturn(idA);
		when(b.getName()).thenReturn("Alpha");
		when(b.getPhone()).thenReturn("2");
		when(b.getAddress()).thenReturn("B");

		given(clinicRepository.findAll()).willReturn(List.of(a, b));

		mvc.perform(get("/clinics").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(idA.toString()))
			.andExpect(jsonPath("$[0].name").value("Alpha"))
			.andExpect(jsonPath("$[1].id").value(idB.toString()))
			.andExpect(jsonPath("$[1].name").value("Beta"));
	}
}
