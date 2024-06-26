package com.uber.egypt.signature;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.StoreException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testing strategy
 * <p>
 * All the tests in this class are about validating the structure of a generated signature by
 * inspecting the signature's components using observer methods and comparing them against an
 * expected value.
 */
public class CadesBesSigningStrategyTest {
    private CadesBesSigningStrategy signingStrategy;

    private HardwareTokenSecurityFactory securityFactory;

    // SHA-256 of input
    private final String input = "c96c6d5be8d08a12e7b5cdc1b207fa6b2430974c86803d8891675e76fd992c20";
    private String base64SignedInput;
    private byte[] signedInput;
    private CMSSignedData signedData;
    private SignerInformationStore signerInfos;
    private SignerInformation signerInfo;
    private AttributeTable signedAttributes;

    @BeforeEach
    public void setup() {
        securityFactory = mock(HardwareTokenSecurityFactory.class);
        var securityProvider = new BouncyCastleProvider();
        Security.addProvider(securityProvider);
        var keyPair = generateKeyPair(securityProvider);
        var certificate = generateCertificate(keyPair);

        when(securityFactory.getPrivateKey()).thenReturn(keyPair.getPrivate());
        when(securityFactory.getCertificate()).thenReturn(certificate);
        when(securityFactory.getProvider()).thenReturn(securityProvider);
        signingStrategy = new CadesBesSigningStrategy(securityFactory);
        base64SignedInput = signingStrategy.sign(input);
        signedInput = Base64.getDecoder().decode(base64SignedInput);
        try {
            signedData = new CMSSignedData(signedInput);
            signerInfos = signedData.getSignerInfos();
            signerInfo = signedData.getSignerInfos().getSigners().iterator().next();
            signedAttributes = signerInfo.getSignedAttributes();
        } catch (CMSException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void signature_should_be_a_CMS_SignedData_signature() {
        // When, then.
        assertDoesNotThrow(() -> new CMSSignedData(signedInput));
    }

    @Test
    public void signedData_version_should_be_3() {
        // When.
        int signedDataVersion = signedData.getVersion();

        // Then.
        then(signedDataVersion).isEqualTo(3);
    }

    @Test
    public void signedData_digestAlgorithms_should_be_SHA256() {
        // Given.
        AlgorithmIdentifier SHA256 = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256);

        // When.
        Set<AlgorithmIdentifier> digestAlgorithms = signedData.getDigestAlgorithmIDs();

        // Then.
        then(digestAlgorithms).contains(SHA256);
    }

    @Test
    public void signedData_encapContentInfo_contentType_should_be_digestData() {
        // Given.
        String digestData = PKCSObjectIdentifiers.digestedData.toString();

        // When.
        String contentType = signedData.getSignedContentTypeOID();

        // Then.
        then(contentType).isEqualTo(digestData);
    }

    @Test
    public void signedData_encapContentInfo_eContent_should_not_be_present() {
        // When.
        boolean isDetachedSignature = signedData.isDetachedSignature();

        // Then.
        then(isDetachedSignature).isTrue();
    }

    @Test
    public void signedData_certificates_should_contain_only_the_X509_certificate_of_the_signer() throws StoreException, IOException {
        // When.
        Store<X509CertificateHolder> certificateStore = signedData.getCertificates();
        Collection<X509CertificateHolder> matches = certificateStore.getMatches(new Selector<>() {

            @Override
            public boolean match(X509CertificateHolder obj) {
                return true;
            }

            @Override
            public Object clone() {
                return null;
            }
        });

        // Then.
        then(matches.size()).isEqualTo(1);
    }

    @Test
    public void signedData_signerInfos_should_contain_only_one_signerInfo_in_the_signature() {
        // Then.
        then(signerInfos.size()).isEqualTo(1);
    }

    @Test
    public void signerInfo_version_should_be_1() {
        // When.
        int signerInfoVersion = signerInfo.getVersion();

        // Then.
        then(signerInfoVersion).isEqualTo(1);
    }

    @Test
    public void signerInfo_sId_should_be_issuerAndSerialNumber_and_should_contain_the_serial_number_of_the_certificate_and_issuer_name() {
        // Given.
        String expectedIssuerName = securityFactory.getCertificate().getIssuerX500Principal().getName();
        Map<String, String> expectedIssuerNameRDNs = getIssuerNameRDNs(expectedIssuerName);
        BigInteger expectedSerialNumber = securityFactory.getCertificate().getSerialNumber();

        // When.
        String actualIssuerName = signerInfo.getSID().getIssuer().toString();
        Map<String, String> actualIssuerNameRDNs = getIssuerNameRDNs(actualIssuerName);
        BigInteger actualSerialNumber = signerInfo.getSID().getSerialNumber();

        // Then.
        then(actualIssuerNameRDNs).isEqualTo(expectedIssuerNameRDNs);
        then(actualSerialNumber).isEqualTo(expectedSerialNumber);
    }

    @Test
    public void signerInfo_digestAlgorithms_should_be_SHA256() {
        // Given.
        String SHA256 = NISTObjectIdentifiers.id_sha256.toString();

        // When.
        String digestAlgorithms = signerInfo.getDigestAlgOID();

        // Then.
        then(digestAlgorithms).isEqualTo(SHA256);
    }

    @Test
    public void signerInfo_signedAttrs_should_contain_at_least_4_attributes() {
        // Then.
        then(signedAttributes.size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    public void signerInfo_signedAttrs_should_contain_a_contentType_attribute() {
        // Given.
        ASN1ObjectIdentifier contentTypeOID = PKCSObjectIdentifiers.pkcs_9_at_contentType;

        // When.
        Attribute contentTypeAttribute = signedAttributes.get(contentTypeOID);

        // Then.
        then(contentTypeAttribute).isNotNull();
    }

    @Test
    public void signerInfo_signedAttrs_should_contain_the_a_messageDigest_attribute() {
        // Given.
        ASN1ObjectIdentifier messageDigestOID = PKCSObjectIdentifiers.pkcs_9_at_messageDigest;

        // When.
        Attribute messageDigestAttribute = signedAttributes.get(messageDigestOID);

        // Then.
        then(messageDigestAttribute).isNotNull();
    }

    @Test
    public void signerInfo_signedAttrs_should_contain_a_signingTime_attribute() {
        // Given.
        ASN1ObjectIdentifier signingTimeOID = PKCSObjectIdentifiers.pkcs_9_at_signingTime;

        // When.
        Attribute signingTimeAttribute = signedAttributes.get(signingTimeOID);

        // Then.
        then(signingTimeAttribute).isNotNull();
    }

    @Test
    public void signerInfo_signedAttrs_should_contain_an_ESSSigningCertificatV2_attribute() {
        // Given.
        ASN1ObjectIdentifier signingCertificateV2OID = PKCSObjectIdentifiers.id_aa_signingCertificateV2;

        // When.
        Attribute ESSSigningCertificatV2Attribute = signedAttributes.get(signingCertificateV2OID);

        // Then.
        then(ESSSigningCertificatV2Attribute).isNotNull();
    }

    @Test
    public void signerInfo_signedAttrs_ContentType_should_be_DigestData() {
        // Given.
        ASN1ObjectIdentifier contentTypeOID = PKCSObjectIdentifiers.pkcs_9_at_contentType;
        ASN1ObjectIdentifier digestedData = PKCSObjectIdentifiers.digestedData;

        // When.
        ASN1Encodable contentType = signedAttributes.get(contentTypeOID).getAttrValues().getObjectAt(0);

        // Then.
        then(contentType).isEqualTo(digestedData);
    }

    @Test
    public void signerInfo_signedAttrs_MessageDigest_should_contain_Der_Octet_String_format_for_SHA256_Hash_of_the_UTF8_encoding_of_the_data_to_be_signed() {
        // Given.
        ASN1ObjectIdentifier messageDigestOID = PKCSObjectIdentifiers.pkcs_9_at_messageDigest;
        DERSet expected = new DERSet(new DEROctetString(Hex.decode(input.getBytes())));

        // When.
        ASN1Encodable actual = signedAttributes.get(messageDigestOID).getAttrValues();

        // then
        then(actual).isEqualTo(expected);
    }

    @Test
    public void signerInfo_signedAttrs_ESSSigningCertificateV2_should_contains_SHA256_hash_of_the_signer_certificate() throws NoSuchAlgorithmException, CertificateEncodingException {
        // Given.
        ASN1ObjectIdentifier signingCertificateV2OID = PKCSObjectIdentifiers.id_aa_signingCertificateV2;
        MessageDigest digester = MessageDigest.getInstance("SHA-256");
        byte[] expected = digester.digest(securityFactory.getCertificate().getEncoded());

        // When.
        ASN1Encodable certificateDigest = signedAttributes.get(signingCertificateV2OID).getAttrValues().getObjectAt(0);
        SigningCertificateV2 signingCertificateV2 = SigningCertificateV2.getInstance(certificateDigest);
        ESSCertIDv2 certIDv2 = signingCertificateV2.getCerts()[0];
        byte[] actual = certIDv2.getCertHash();

        // Then.
        then(actual).isEqualTo(expected);
    }

    @Test
    public void signerInfo_signedAttrs_SigningTime_should_be_the_machine_time_in_UTC() throws CMSException, ParseException, InterruptedException {
        // Given.
        ASN1ObjectIdentifier signingTimeOID = PKCSObjectIdentifiers.pkcs_9_at_signingTime;
        final int UTCTIME_LOWEST_TIME_RESOLUTION_IN_SECONDS = 1;
        Date before = new Date();
        TimeUnit.SECONDS.sleep(UTCTIME_LOWEST_TIME_RESOLUTION_IN_SECONDS);
        base64SignedInput = signingStrategy.sign(input);
        signedInput = Base64.getDecoder().decode(base64SignedInput);
        CMSSignedData signedData = new CMSSignedData(signedInput);
        TimeUnit.SECONDS.sleep(UTCTIME_LOWEST_TIME_RESOLUTION_IN_SECONDS);
        Date after = new Date();

        // When.
        SignerInformation signerInfo = signedData.getSignerInfos().getSigners().iterator().next();
        ASN1Encodable signingTime = signerInfo.getSignedAttributes().get(signingTimeOID).getAttrValues().getObjectAt(0);

        ASN1UTCTime ASN1UTCTime = org.bouncycastle.asn1.ASN1UTCTime.getInstance(signingTime);
        Date date = ASN1UTCTime.getDate();

        // Then.
        then(before.before(date)).isTrue();
        then(after.after(date)).isTrue();
    }

    @Test
    public void signerInfo_signatureAlgorithm_SignatureAlgorithmIdentifier_should_be_sha256WithRSAEncryption() {
        // Given.
        String sha256WithRSAEncryption = PKCSObjectIdentifiers.sha256WithRSAEncryption.toString();

        // When.
        String signatureAlgorithmIdentifier = signerInfo.getEncryptionAlgOID();

        // Then.
        then(signatureAlgorithmIdentifier).isEqualTo(sha256WithRSAEncryption);
    }

    @Test
    public void signerInfo_Signature_should_be_Signature_value_computed_on_the_user_data_and_on_the_signed_attributes_using_the_signer_private_key_with_Algorithm_sha256WithRSAEncryption() throws IOException, SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        // Given.
        byte[] signature = signerInfo.getSignature();
        Signature verifier = Signature.getInstance("SHA256withRSAEncryption", securityFactory.getProvider());
        verifier.initVerify(securityFactory.getCertificate().getPublicKey());

        // When.
        byte[] encodedSignedAttributes = signerInfo.getEncodedSignedAttributes();
        verifier.update(encodedSignedAttributes);
        boolean verified = verifier.verify(signature);

        // Then.
        then(verified).isTrue();
    }

    @Test
    public void signerInfo_unsignedAttrs_should_not_be_present() {
        // When.
        AttributeTable unsignedAttributes = signerInfo.getUnsignedAttributes();

        // Then.
        then(unsignedAttributes).isNull();
    }

    private Map<String, String> getIssuerNameRDNs(String issuerName) {
        Map<String, String> result = new HashMap<>();
        String[] RDNs = issuerName.split(",");
        for (String RDN : RDNs) {
            String RDNKey = RDN.split("=")[0];
            String RDNValue = RDN.split("=")[1];
            result.put(RDNKey, RDNValue);
        }
        return result;
    }

    private KeyPair generateKeyPair(Provider provider) {
        try {
            var keyGen = KeyPairGenerator.getInstance("RSA", provider);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private X509Certificate generateCertificate(KeyPair keyPair) {
        try {
            var certHldr = SecurityUtils.createTrustAnchor(keyPair, "SHA256withRSAEncryption");
            return SecurityUtils.convertX509CertificateHolder(certHldr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
