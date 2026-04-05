package org.springframework.samples.petclinic.visits.web;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;


import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VisitResource.class)
@ActiveProfiles("test")
class VisitResourceTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    VisitRepository visitRepository;

    private static final UUID P1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID P2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldFetchVisits() throws Exception {
        given(visitRepository.findByPetIdIn(asList(P1, P2)))
            .willReturn(
                asList(
                    Visit.VisitBuilder.aVisit()
                        .id(1)
                        .petId(P1)
                        .build(),
                    Visit.VisitBuilder.aVisit()
                        .id(2)
                        .petId(P2)
                        .build(),
                    Visit.VisitBuilder.aVisit()
                        .id(3)
                        .petId(P2)
                        .build()
                )
            );

        mvc.perform(get("/pets/visits?petId=" + P1 + "," + P2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].id").value(1))
            .andExpect(jsonPath("$.items[1].id").value(2))
            .andExpect(jsonPath("$.items[2].id").value(3))
            .andExpect(jsonPath("$.items[0].petId").value(P1.toString()))
            .andExpect(jsonPath("$.items[1].petId").value(P2.toString()))
            .andExpect(jsonPath("$.items[2].petId").value(P2.toString()));
    }
}
