package mx.cdefis.cfdi.sign;

import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class CfdiSigner {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // Obtener PrivateKey desde .key + password
    public static PrivateKey getPrivateKey(String keyPath, String password) throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        byte[] keyBytes = Files.readAllBytes(Paths.get(keyPath));

        // Detecta si es PKCS#8 cifrado
        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo =
                new PKCS8EncryptedPrivateKeyInfo(keyBytes);

        InputDecryptorProvider decryptorProvider =
                new JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .build(password.toCharArray());

        PrivateKeyInfo privateKeyInfo =
                encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider);

        return new JcaPEMKeyConverter()
                .setProvider("BC")
                .getPrivateKey(privateKeyInfo);
    }

    //  Generar sello
    public static String generarSello(String cadenaOriginal, String keyPath, String password) throws Exception {

        PrivateKey privateKey = getPrivateKey(keyPath, password);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(cadenaOriginal.getBytes("UTF-8"));

        byte[] signed = signature.sign();

        return Base64.getEncoder().encodeToString(signed);
    }

    // Certificado en Base64
    public static String getCertificadoBase64(String cerName) {
        try (InputStream is = CfdiSigner.class.getClassLoader()
                .getResourceAsStream("csd/" + cerName)) {

            if (is == null) {
                throw new RuntimeException("No se encontró el archivo .cer");
            }

            byte[] certBytes = is.readAllBytes();
            return Base64.getEncoder().encodeToString(certBytes);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Número de certificado
    public static String getNoCertificado(String cerName) {
        try (InputStream is = CfdiSigner.class.getClassLoader()
                .getResourceAsStream("csd/" + cerName)) {

            if (is == null) {
                throw new RuntimeException("No se encontró el archivo .cer");
            }

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(is);

            String serial = cert.getSerialNumber().toString();

            // Tomar solo los últimos 20 dígitos
            if (serial.length() > 20) {
                serial = serial.substring(serial.length() - 20);
            }

            return serial;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}