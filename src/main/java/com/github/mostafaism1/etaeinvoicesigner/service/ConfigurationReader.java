package com.github.mostafaism1.etaeinvoicesigner.service;

public interface ConfigurationReader {
  public String getPkcs11ConfigFilePath();

  public String getKeyStorePath();

  public String getKeyStorePassword();

  public String getCertificateIssuerName();

  public String getUserName();

  public String getEncryptedPassword();
}
