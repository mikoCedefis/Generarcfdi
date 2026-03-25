package mx.cdefis.cfdi;

import mx.cdefis.cfdi.builder.CfdiBuilder;
import mx.cdefis.cfdi.model.Comprobante;
import mx.cdefis.cfdi.model.InstEducativas;
import mx.cdefis.cfdi.cadena.CfdiCadenaOriginalGenerator;
import mx.cdefis.cfdi.sign.CfdiSigner;
import mx.cdefis.cfdi.timbrado.FinkokTimbrador;
import mx.cdefis.cfdi.util.NamespacePrefixMapperImpl;
import mx.cdefis.cfdi.validator.CfdiValidator;
import mx.cdefis.cfdi.util.XmlUtils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import java.io.File;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class CfdiProcessor {

    public static void process(CfdiBuilder builder, boolean usarIEDU) throws Exception {

        Comprobante comprobante = builder.build();

        // =====================
        // JAXB CONTEXT
        // =====================
        JAXBContext context = usarIEDU
                ? JAXBContext.newInstance(Comprobante.class, InstEducativas.class)
                : JAXBContext.newInstance(Comprobante.class);

        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(
                "org.glassfish.jaxb.namespacePrefixMapper",
                new NamespacePrefixMapperImpl()
        );

        // =====================
        // SCHEMA LOCATION
        // =====================
        if (usarIEDU) {
            marshaller.setProperty(
                    Marshaller.JAXB_SCHEMA_LOCATION,
                    "http://www.sat.gob.mx/cfd/4 " +
                            "http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd " +
                            "http://www.sat.gob.mx/iedu " +
                            "http://www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xsd"
            );
        } else {
            marshaller.setProperty(
                    Marshaller.JAXB_SCHEMA_LOCATION,
                    "http://www.sat.gob.mx/cfd/4 " +
                            "http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd"
            );
        }

        // =====================
        // CERTIFICADO
        // =====================
        String keyPath = "src/main/resources/csd/prueba.key";
        String cerName = "prueba.cer";
        String password = "12345678a";

        String certificado = CfdiSigner.getCertificadoBase64(cerName);
        String noCertificado = CfdiSigner.getNoCertificado(cerName);

        comprobante.setCertificado(certificado);
        comprobante.setNoCertificado(noCertificado);

        // =====================
        // DIRECTORIO TEMPORAL
        // =====================
        Path tempDir = Path.of("logs/xml");
        Files.createDirectories(tempDir);

        Path xmlSinSello = tempDir.resolve("cfdi.xml");
        Path xmlFirmadoPath = tempDir.resolve("cfdi_firmado.xml");

        // =====================
        // XML SIN SELLO
        // =====================
        marshaller.marshal(comprobante, xmlSinSello.toFile());

        // =====================
        // CADENA ORIGINAL
        // =====================
        String cadena = CfdiCadenaOriginalGenerator.generarCadena(
                xmlSinSello.toString(),
                "src/main/resources/xslt/cadenaoriginal_4_0.xslt"
        );

        System.out.println("Cadena Original:");
        System.out.println(cadena);

        // =====================
        // SELLO
        // =====================
        String sello = CfdiSigner.generarSello(cadena, keyPath, password);
        comprobante.setSello(sello);

        // =====================
        // XML FIRMADO
        // =====================
        marshaller.marshal(comprobante, xmlFirmadoPath.toFile());

        // =====================
        // TIMBRADO
        // =====================
        FinkokTimbrador timbrador = new FinkokTimbrador();

        try {
            String xmlFirmado = Files.readString(xmlFirmadoPath);
            String xmlTimbrado = timbrador.timbrar(xmlFirmado);

            String uuid = XmlUtils.extraerUUID(xmlTimbrado);

            LocalDate fecha = LocalDate.now();
            Path baseDir = Path.of("storage/cfdi/timbrados");

            Path dir = baseDir
                    .resolve(String.valueOf(fecha.getYear()))
                    .resolve(String.format("%02d", fecha.getMonthValue()));

            Files.createDirectories(dir);

            Path file = dir.resolve(uuid + ".xml");



            Files.writeString(file, xmlTimbrado, StandardCharsets.UTF_8);

            System.out.println("XML guardado en: " + file.toAbsolutePath());

            // =====================
            // VALIDACIÓN
            // =====================
            if (usarIEDU) {
                CfdiValidator.validate(
                        file.toString(),
                        "src/main/resources/xsd/cfdv40.xsd",
                        "src/main/resources/xsd/iedu.xsd",
                        "src/main/resources/xsd/TimbreFiscalDigitalv11.xsd"
                );
            } else {
                CfdiValidator.validate(
                        file.toString(),
                        "src/main/resources/xsd/cfdv40.xsd",
                        "src/main/resources/xsd/TimbreFiscalDigitalv11.xsd"
                );
            }

        } catch (Exception e) {

            // =====================
            // LOG DE ERRORES
            // =====================
            Path errorDir = Path.of("logs/errors");
            Files.createDirectories(errorDir);

            String ts = String.valueOf(System.currentTimeMillis());

            Files.writeString(
                    errorDir.resolve("cfdi_error_" + ts + ".xml"),
                    Files.readString(xmlSinSello),
                    StandardCharsets.UTF_8
            );

            Files.writeString(
                    errorDir.resolve("cfdi_firmado_error_" + ts + ".xml"),
                    Files.readString(xmlFirmadoPath),
                    StandardCharsets.UTF_8
            );

            System.out.println("❌ Error en timbrado. XMLs guardados para debug");

            throw e;
        }

        System.out.println("✅ CFDI timbrado correctamente");
    }
}