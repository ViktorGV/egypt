package com.uber.egypt;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;

public class CustomContentSigner implements ContentSigner {

    private final AlgorithmIdentifier algorithmIdentifier;
    private final Signature signature;
    private final ByteArrayOutputStream outputStream;

    public CustomContentSigner(PrivateKey privateKey, String sigAlgo) {
        //Utils.throwIfNull(privateKey, sigAlgo);
        this.algorithmIdentifier = new DefaultSignatureAlgorithmIdentifierFinder().find(sigAlgo);

        try {
            this.outputStream = new ByteArrayOutputStream();
            this.signature = Signature.getInstance("NONEwithRSA");
            this.signature.initSign(privateKey);
        } catch (GeneralSecurityException gse) {
            throw new IllegalArgumentException(gse.getMessage());
        }
    }

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return algorithmIdentifier;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public byte[] getSignature() {
        try {
            signature.update(outputStream.toByteArray());
            return signature.sign();
        } catch (GeneralSecurityException gse) {
            gse.printStackTrace();
            return null;
        }
    }
}
