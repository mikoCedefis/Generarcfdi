package mx.cdefis.cfdi;

import mx.cdefis.cfdi.model.Comprobante;
import mx.cdefis.cfdi.model.InstEducativas;
import mx.cdefis.cfdi.util.NamespacePrefixMapperImpl;
import mx.cdefis.cfdi.validator.CfdiValidator;
import mx.cdefis.cfdi.cadena.CfdiCadenaOriginalGenerator;
import mx.cdefis.cfdi.sign.CfdiSigner;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import java.io.File;

public class CfdiProcessor {

    public static void process(Comprobante comprobante, String xsd) throws Exception {


        // Validar el CFDI
        JAXBContext context = JAXBContext.newInstance(
                Comprobante.class,
                InstEducativas.class
                );
        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(
                "org.glassfish.jaxb.namespacePrefixMapper",
                new NamespacePrefixMapperImpl()
        );

        marshaller.setProperty(
                jakarta.xml.bind.Marshaller.JAXB_SCHEMA_LOCATION,
                "http://www.sat.gob.mx/cfd/4 " +
                        "http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd " +
                        "http://www.sat.gob.mx/iedu " +
                        "http://www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xsd"
        );

        // Certificado
        String keyPath = "src/main/resources/csd/prueba.key";
        String cerName = "prueba.cer";
        String password = "12345678a";

        String certificado = CfdiSigner.getCertificadoBase64(cerName);
        String noCertificado = CfdiSigner.getNoCertificado(cerName);

        comprobante.setCertificado(certificado);
        comprobante.setNoCertificado(noCertificado);

        // xml sin sello
        marshaller.marshal(comprobante, new File("cfdi.xml"));

        // Cadena Original
        String cadena = CfdiCadenaOriginalGenerator.generarCadena(
                "cfdi.xml",
                "src/main/resources/xslt/cadenaoriginal_4_0.xslt"
        );

        System.out.println("Cadena Original:");
        System.out.println(cadena);

        // Sello
        String sello = CfdiSigner.generarSello(cadena, keyPath, password);
        comprobante.setSello(sello);

        // xml con sello
        marshaller.marshal(comprobante, new File("cfdi_firmado.xml"));
        marshaller.marshal(comprobante, System.out);

        // Validar el CFDI
        CfdiValidator.validate(
                "cfdi_firmado.xml",
                "src/main/resources/xsd/cfdv40.xsd",
                "src/main/resources/xsd/iedu.xsd"
        );

        //System.out.println("CFDI validado correctamente.");

    }
}