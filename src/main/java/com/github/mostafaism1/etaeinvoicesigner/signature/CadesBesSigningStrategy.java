package com.github.mostafaism1.etaeinvoicesigner.signature;

import com.github.mostafaism1.etaeinvoicesigner.signature.security.HardwareTokenSecurityFactory;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;

@Component
public class CadesBesSigningStrategy {
    private static final Provider DIGEST_PROVIDER = new BouncyCastleProvider();
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSAEncryption";
    private final Provider signatureProvider;
    private final PrivateKey signingKey;
    private final Certificate signingCert;

    public CadesBesSigningStrategy(HardwareTokenSecurityFactory hardwareTokenSecurityFactory) {
        this.signatureProvider = hardwareTokenSecurityFactory.getProvider();
        this.signingKey = hardwareTokenSecurityFactory.getPrivateKey();
        this.signingCert = hardwareTokenSecurityFactory.getCertificate();
    }

    public String sign(String data) {
        CMSSignedData signedData;
        try {
            signedData = buildCMSSignedData(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signedData.getEncoded());
        } catch (Exception e) {
            throw new SignatureException(e);
        }
    }

    public CMSSignedData buildCMSSignedData(byte[] msg) throws CertificateEncodingException, NoSuchAlgorithmException, OperatorCreationException, IOException, CMSException {
        var signedDataGenerator = buildCMSSignedDataGenerator(msg);
        var cmsTypedData = new CMSProcessableByteArray(PKCSObjectIdentifiers.digestedData, msg);
        return signedDataGenerator.generate(cmsTypedData, false);
    }

    private CMSSignedDataGenerator buildCMSSignedDataGenerator(byte[] msg) throws CertificateEncodingException, OperatorCreationException, NoSuchAlgorithmException, IOException, CMSException {
        var signerInfoGenerator = buildSignerInfoGenerator(msg);
        var signedDataGenerator = new CMSSignedDataGenerator();
        signedDataGenerator.addSignerInfoGenerator(signerInfoGenerator);
        signedDataGenerator.addCertificate(new X509CertificateHolder(signingCert.getEncoded()));
        return signedDataGenerator;
    }

    private SignerInfoGenerator buildSignerInfoGenerator(byte[] msg) throws CertificateEncodingException, NoSuchAlgorithmException, OperatorCreationException, IOException {
        var signedAttributesTable = buildSignedAttributeTable(msg);

        var signedAttributeGenerator = new DefaultSignedAttributeTableGenerator(signedAttributesTable);

        var contentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(signatureProvider).build(signingKey);

        var digestCalcProvider = new JcaDigestCalculatorProviderBuilder().setProvider(DIGEST_PROVIDER).build();
        return new SignerInfoGeneratorBuilder(digestCalcProvider).setSignedAttributeGenerator(signedAttributeGenerator).setUnsignedAttributeGenerator(null).build(contentSigner, new X509CertificateHolder(signingCert.getEncoded()));
    }

    private AttributeTable buildSignedAttributeTable(byte[] msg) throws NoSuchAlgorithmException, CertificateEncodingException {
        ASN1EncodableVector signedAttributes = new ASN1EncodableVector();
        signedAttributes.add(buildMessageDigestAttribute(msg));
        signedAttributes.add(buildSigningCertificateV2Attribute());
        return new AttributeTable(signedAttributes);
    }

    private ASN1Encodable buildMessageDigestAttribute(byte[] msg) throws NoSuchAlgorithmException {
        var digest = MessageDigest.getInstance(DIGEST_ALGORITHM).digest(msg);

        var attributeIdentifier = ASN1ObjectIdentifier.getInstance(PKCSObjectIdentifiers.pkcs_9_at_messageDigest);
        var attributeValue = new DERSet(new DEROctetString(digest));
        return new Attribute(attributeIdentifier, attributeValue);
    }

    private Attribute buildSigningCertificateV2Attribute() throws CertificateEncodingException, NoSuchAlgorithmException {
        var digester = MessageDigest.getInstance(DIGEST_ALGORITHM);

        var attributeIdentifier = ASN1ObjectIdentifier.getInstance(PKCSObjectIdentifiers.id_aa_signingCertificateV2);
        var essCert = new ESSCertIDv2(new AlgorithmIdentifier(attributeIdentifier), digester.digest(signingCert.getEncoded()));

        var signingCertificateV2 = new SigningCertificateV2(new ESSCertIDv2[]{essCert});
        var attributeValue = new DERSet(signingCertificateV2);

        return new Attribute(attributeIdentifier, attributeValue);
    }

    private static class SignatureException extends RuntimeException {
        public SignatureException(Exception e) {
            super(e);
        }
    }
}
