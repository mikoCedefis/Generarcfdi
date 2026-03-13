 package mx.cdefis.cfdi.sign;

import java.io.File;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class CfdiSigner {

    public static String generarSello(String cadenaOriginal, String keyPath) throws Exception {

        byte[] keyBytes = Files.readAllBytes(new File(keyPath).toPath());

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        PrivateKey privateKey = keyFactory.generatePrivate(spec);

        Signature signature = Signature.getInstance("SHA256withRSA");

        signature.initSign(privateKey);

        signature.update(cadenaOriginal.getBytes("UTF-8"));

        byte[] signed = signature.sign();

        return Base64.getEncoder().encodeToString(signed);
    }

    public static String getCertificadoBase64(String cerPath) throws Exception {

        byte[] certBytes = Files.readAllBytes(new File(cerPath).toPath());

        return Base64.getEncoder().encodeToString(certBytes);
    }

    public static String getNoCertificado(String cerPath) throws Exception {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        X509Certificate cert = (X509Certificate) cf.generateCertificate(
                Files.newInputStream(new File(cerPath).toPath())
        );

        return cert.getSerialNumber().toString();
    }
}
