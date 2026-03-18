package org.springframework.samples.petclinic.api.boundary.web;

import org.apache.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping
    @PostMapping
    public ResponseEntity<String> fallback() {
        return ResponseEntity.status(HttpStatus.SC_SERVICE_UNAVAILABLE)
                .body("Service is temporarily unavailable. Please try again later.");
    }
}
