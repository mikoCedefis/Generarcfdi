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
        JAXBContext context;

        if (usarIEDU) {
            context = JAXBContext.newInstance(Comprobante.class, InstEducativas.class);
        } else {
            context = JAXBContext.newInstance(Comprobante.class);
        }

        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(
                "org.glassfish.jaxb.namespacePrefixMapper",
                new NamespacePrefixMapperImpl()
        );

        // =====================
        // SCHEMA LOCATION (CLAVE)
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
        // XML SIN SELLO
        // =====================
        marshaller.marshal(comprobante, new File("cfdi.xml"));

        // =====================
        // CADENA ORIGINAL
        // =====================
        String cadena = CfdiCadenaOriginalGenerator.generarCadena(
                "cfdi.xml",
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
        marshaller.marshal(comprobante, new File("cfdi_firmado.xml"));

        // =====================
        // TIMBRADO
        // =====================
        FinkokTimbrador timbrador = new FinkokTimbrador();

        String xmlFirmado = Files.readString(Path.of("cfdi_firmado.xml"));
        String xmlTimbrado = timbrador.timbrar(xmlFirmado);


        String uuid = XmlUtils.extraerUUID(xmlTimbrado);

        // Fecha actual (puedes cambiarla por la del comprobante si quieres)
        LocalDate fecha = LocalDate.now();

        // Ruta base correcta
        Path baseDir = Path.of("storage/cfdi/timbrados");

        // Año y mes
        String year = String.valueOf(fecha.getYear());
        String month = String.format("%02d", fecha.getMonthValue());

        // Construcción de carpeta
        Path dir = baseDir.resolve(year).resolve(month);

        // Crear carpetas automáticamente
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // Archivo final (UUID.xml)
        Path file = dir.resolve(uuid + ".xml");

        // Guardar XML
        Files.writeString(file, xmlTimbrado, StandardCharsets.UTF_8);

        System.out.println("XML guardado en: " + file.toAbsolutePath());

        // =====================
        // VALIDACIÓN
        // =====================
        if (usarIEDU) {
            CfdiValidator.validate(
                    "cfdi_timbrado.xml",
                    "src/main/resources/xsd/cfdv40.xsd",
                    "src/main/resources/xsd/iedu.xsd",
                    "src/main/resources/xsd/TimbreFiscalDigitalv11.xsd"
            );
        } else {
            CfdiValidator.validate(
                    "cfdi_timbrado.xml",
                    "src/main/resources/xsd/cfdv40.xsd",
                    "src/main/resources/xsd/TimbreFiscalDigitalv11.xsd"
            );
        }

        System.out.println("✅ CFDI timbrado correctamente");
    }
}