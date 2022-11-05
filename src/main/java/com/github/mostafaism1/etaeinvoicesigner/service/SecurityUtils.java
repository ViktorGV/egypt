package com.github.mostafaism1.etaeinvoicesigner.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class SecurityUtils {
  static long serialNumberBase = System.currentTimeMillis();

  /**
   * Calculate a serial number using a monotonically increasing value.
   *
   * @return a BigInteger representing the next serial number in the sequence.
   */
  public static synchronized BigInteger calculateSerialNumber() {
    return BigInteger.valueOf(SecurityUtils.serialNumberBase++);
  }

  /**
   * Calculate a date in seconds (suitable for the PKIX profile - RFC 5280)
   *
   * @param hoursInFuture hours ahead of now, may be negative.
   * @return a Date set to now + (hoursInFuture * 60 * 60) seconds
   */
  public static Date calculateDate(int hoursInFuture) {
    long secs = System.currentTimeMillis() / 1000;
    return new Date((secs + (hoursInFuture * 60 * 60)) * 1000);
  }

  /**
   * Simple method to convert an X509CertificateHolder to an X509Certificate
   * using the java.security.cert.CertificateFactory class.
   */
  public static X509Certificate convertX509CertificateHolder(
    X509CertificateHolder certHolder
  )
    throws GeneralSecurityException, IOException {
    CertificateFactory cFact = CertificateFactory.getInstance("X.509", "BC");
    return (X509Certificate) cFact.generateCertificate(
      new ByteArrayInputStream(certHolder.getEncoded())
    );
  }

  /**
  * Build a sample self-signed V1 certificate to use as a trust anchor, or
  * root certificate.
  *
  * @param keyPair the key pair to use for signing and providing the
  *
  public key.
  * @param sigAlg the signature algorithm to sign the certificate with.
  * @return an X509CertificateHolder containing the V1 certificate.
  */
  public static X509CertificateHolder createTrustAnchor(
    KeyPair keyPair,
    String sigAlg
  )
    throws OperatorCreationException {
    X500NameBuilder x500NameBld = new X500NameBuilder(BCStyle.INSTANCE)
      .addRDN(BCStyle.C, "AU")
      .addRDN(BCStyle.ST, "Victoria")
      .addRDN(BCStyle.L, "Melbourne")
      .addRDN(BCStyle.O, "The Legion of the Bouncy Castle")
      .addRDN(BCStyle.CN, "Demo Root Certificate");
    X500Name name = x500NameBld.build();
    X509v1CertificateBuilder certBldr = new JcaX509v1CertificateBuilder(
      name,
      calculateSerialNumber(),
      calculateDate(0),
      calculateDate(24 * 31),
      name,
      keyPair.getPublic()
    );
    ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
      .setProvider("BC")
      .build(keyPair.getPrivate());
    return certBldr.build(signer);
  }
}
