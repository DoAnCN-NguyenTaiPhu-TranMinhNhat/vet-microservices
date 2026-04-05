package org.springframework.samples.petclinic.customers.web;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.auth.ClinicScopeService;
import org.springframework.samples.petclinic.customers.config.SecurityConfiguration;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.model.Pet;
import org.springframework.samples.petclinic.customers.model.PetRepository;
import org.springframework.samples.petclinic.customers.model.PetType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Maciej Szarlinski
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(PetResource.class)
@Import(SecurityConfiguration.class)
@ActiveProfiles("test")
class PetResourceTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PetRepository petRepository;

    @MockBean
    OwnerRepository ownerRepository;

    @MockBean
    ClinicScopeService clinicScopeService;

    private static final UUID PET_ID = UUID.fromString("6cd5ce45-69f2-50ee-a996-db54a21c49c0");

    @Test
    void shouldCreatePetWithoutIdInJsonBody() throws Exception {
        UUID ownerId = UUID.fromString("34913c52-827f-4b75-a9ae-97d0754ca1f9");
        Owner owner = new Owner();
        Field idField = Owner.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(owner, ownerId);
        owner.setFirstName("A");
        owner.setLastName("B");

        PetType cat = new PetType();
        cat.setId(1);
        cat.setName("cat");

        given(ownerRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(petRepository.findPetTypeById(1)).willReturn(Optional.of(cat));
        given(petRepository.save(any(Pet.class))).willAnswer(invocation -> {
            Pet p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
            }
            return p;
        });

        String body = "{\"name\":\"Fluffy\",\"birthDate\":\"2020-01-15\",\"typeId\":1,\"gender\":\"male\",\"vaccinationStatus\":\"yes\",\"medicalNotes\":\"\"}";
        mvc.perform(post("/owners/" + ownerId + "/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());

        // Extra "id" in body is ignored — PetCreateRequest has no id field (no UUID parse error).
        String withIgnoredId = "{\"id\":0,\"name\":\"ExtraIdIgnored\",\"birthDate\":\"2020-01-15\",\"typeId\":1,\"gender\":\"\",\"vaccinationStatus\":\"\",\"medicalNotes\":\"\"}";
        mvc.perform(post("/owners/" + ownerId + "/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(withIgnoredId))
            .andExpect(status().isCreated());
    }

    @Test
    void shouldGetAPetInJSonFormat() throws Exception {

        Pet pet = setupPet();

        given(petRepository.findById(PET_ID)).willReturn(Optional.of(pet));


        mvc.perform(get("/owners/" + UUID.randomUUID() + "/pets/" + PET_ID).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(PET_ID.toString()))
            .andExpect(jsonPath("$.name").value("Basil"))
            .andExpect(jsonPath("$.type.id").value(6));
    }

    private Pet setupPet() {
        Owner owner = new Owner();
        owner.setFirstName("George");
        owner.setLastName("Bush");

        Pet pet = new Pet();

        pet.setName("Basil");
        pet.setId(PET_ID);

        PetType petType = new PetType();
        petType.setId(6);
        pet.setType(petType);

        owner.addPet(pet);
        return pet;
    }
}
