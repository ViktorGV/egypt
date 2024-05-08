package com.uber.egypt.controller;

import com.uber.egypt.signature.DocumentSigningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sign")
public class SignatureController {
    private final DocumentSigningService documentSigningService;

    protected SignatureController(DocumentSigningService documentSigningService) {
        this.documentSigningService = documentSigningService;
    }

    @PostMapping
    public ResponseEntity<String> signDocuments(@RequestBody String jsonDocuments) {
        return ResponseEntity.ok(documentSigningService.generateSignedDocuments(jsonDocuments));
    }
}
