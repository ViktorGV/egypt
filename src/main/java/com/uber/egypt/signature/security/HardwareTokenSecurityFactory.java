package com.uber.egypt.signature.security;

import com.uber.egypt.configuration.FileConfigurationReader;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

@Component
public class HardwareTokenSecurityFactory {
    private static final String PROVIDER_NAME = "SunPKCS11";
    private static final String KEY_STORE_TYPE = "PKCS11";

    private final FileConfigurationReader configurationReader;
    private Provider provider;
    private KeyStore keyStore;
    private final String alias;

    HardwareTokenSecurityFactory() {
        configurationReader = FileConfigurationReader.INSTANCE;
        provider = Security.getProvider(PROVIDER_NAME);
        addSecurityProvider();
        initializeKeystore();
        alias = getAliasByCertificateIssuerName(
                keyStore,
                configurationReader.getCertificateIssuerName()
        );
    }

    public void addSecurityProvider() {
        provider =
                provider.configure(configurationReader.getPkcs11ConfigFilePath());
        Security.addProvider(provider);
    }

    public PrivateKey getPrivateKey() {
        try {
            return (PrivateKey) keyStore.getKey(alias, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public X509Certificate getCertificate() {
        try {
            return (X509Certificate) keyStore.getCertificate(
                    alias
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Provider getProvider() {
        return provider;
    }

    private void initializeKeystore() {
        try {
            keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            keyStore.load(
                    null,
                    configurationReader.getKeyStorePassword().toCharArray()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getAliasByCertificateIssuerName(KeyStore keyStore, String targetIssuerName) {
        try {
            Enumeration<String> aliases;
            aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
                String issuerName = certificate.getIssuerX500Principal().getName();
                if (issuerName.contains(targetIssuerName)) {
                    return alias;
                }
            }
            throw new RuntimeException();
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
