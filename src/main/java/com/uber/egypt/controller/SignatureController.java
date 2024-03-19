package com.uber.egypt.controller;

import com.uber.egypt.signature.CadesBesSigningStrategy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sign")
public class SignatureController {
    private final CadesBesSigningStrategy cadesBesSigningStrategy;

    protected SignatureController(CadesBesSigningStrategy cadesBesSigningStrategy) {
        this.cadesBesSigningStrategy = cadesBesSigningStrategy;
    }

    @PostMapping
    public ResponseEntity<String> signDocuments(@RequestBody String digest) {
        return ResponseEntity.ok(cadesBesSigningStrategy.sign(digest));
    }
}
