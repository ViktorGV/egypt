package com.uber.egypt.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public enum FileConfigurationReader {
    INSTANCE;

    private static final String CONFIG_FILE_NAME = "application.properties";
    private final Properties properties;

    FileConfigurationReader() {
        properties = tryReadConfiguration();
    }


    public String getPkcs11ConfigFilePath() {
        return properties.getProperty("signature.keystore.pkcs11ConfigFilePath");
    }

    public String getKeyStorePassword() {
        return properties.getProperty("signature.keystore.password");
    }

    public String getCertificateIssuerName() {
        return properties.getProperty("signature.keystore.certificateIssuerName");
    }

    public String getUserName() {
        return properties.getProperty("auth.user.userName");
    }

    public String getEncryptedPassword() {
        return properties.getProperty("auth.user.encryptedPassword");
    }

    private Properties tryReadConfiguration() {
        try {
            return readConfiguration();
        } catch (IOException e) {
            throw new NoConfigurationFoundException();
        }
    }

    private Properties readConfiguration() throws IOException {
        Properties properties = new Properties();
        InputStream propertiesResource =
                FileConfigurationReader.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
        properties.load(propertiesResource);
        return properties;
    }

    private static class NoConfigurationFoundException extends RuntimeException {

        public NoConfigurationFoundException() {
            super();
        }
    }
}
