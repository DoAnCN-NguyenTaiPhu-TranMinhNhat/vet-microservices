package org.springframework.samples.petclinic.genai;

import org.springframework.samples.petclinic.genai.dto.AiDiagnosisRequest;
import org.springframework.samples.petclinic.genai.dto.AiDiagnosisResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class DiagnosisController {

    private final AiDiagnosisClient aiDiagnosisClient;

    public DiagnosisController(AiDiagnosisClient aiDiagnosisClient) {
        this.aiDiagnosisClient = aiDiagnosisClient;
    }

    @PostMapping("/diagnosis")
    public AiDiagnosisResponse diagnosis(@RequestBody AiDiagnosisRequest request) {
        return aiDiagnosisClient.predict(request);
    }
}
