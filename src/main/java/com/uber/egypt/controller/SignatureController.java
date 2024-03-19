package com.uber.egypt.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.uber.egypt.signature.DocumentSigningService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/")
public class SignatureController {
  private DocumentSigningService documentSigningService;

  @PostMapping
  public String signDocuments(@RequestBody String jsonDocuments) {
    return documentSigningService.generateSignedDocuments(jsonDocuments);
  }
}
