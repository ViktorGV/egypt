package com.github.mostafaism1.etaeinvoicesigner.service;

import org.springframework.stereotype.Component;

@Component
public class ETADocumentSigningFactory implements DocumentSigningFactory {

    @Override
    public CanonicalizationStrategy getCanonicalizationStrategy() {
        return new ETAJsonCanonicalizationStrategy();
    }

    @Override
    public SigningStrategy getSigningStrategy() {
        return new ETASigningStrategyB(getSecurityFactory());
    }

    @Override
    public SignatureMergeStrategy getSignatureMergeStrategy() {
        return new ETASignatureMergeStrategy();
    }

    @Override
    public SecurityFactory getSecurityFactory() {
        return new PKCS11SecurityFactory();
    }

}
