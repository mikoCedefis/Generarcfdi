package mx.cdefis.cfdi;

import mx.cdefis.cfdi.model.*;
import mx.cdefis.cfdi.validator.CfdiValidator;
import mx.cdefis.cfdi.cadena.CfdiCadenaOriginalGenerator;
import mx.cdefis.cfdi.sign.CfdiSigner;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConstants;

import java.io.File;
import java.math.BigDecimal;
import java.util.GregorianCalendar;

public class Main {

    public static void main(String[] args) throws Exception {

        Comprobante comprobante = new Comprobante();

        // FECHA CFDI
        GregorianCalendar cal = new GregorianCalendar();

        XMLGregorianCalendar fecha =
                DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);

        fecha.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        fecha.setTimezone(DatatypeConstants.FIELD_UNDEFINED);

        comprobante.setVersion("4.0");
        comprobante.setTipoDeComprobante(CTipoDeComprobante.I);
        comprobante.setFecha(fecha);
        comprobante.setSubTotal(new BigDecimal("100.00"));
        comprobante.setTotal(new BigDecimal("100.00"));
        comprobante.setMoneda(CMoneda.MXN);
        comprobante.setLugarExpedicion("64000");

        // EMISOR
        Comprobante.Emisor emisor = new Comprobante.Emisor();
        emisor.setRfc("AAA010101AAA");
        emisor.setNombre("EMPRESA DE PRUEBA SA DE CV");
        emisor.setRegimenFiscal("601");

        comprobante.setEmisor(emisor);

        // RECEPTOR
        Comprobante.Receptor receptor = new Comprobante.Receptor();
        receptor.setRfc("XAXX010101000");
        receptor.setNombre("PUBLICO GENERAL");
        receptor.setUsoCFDI(CUsoCFDI.G_03);
        receptor.setDomicilioFiscalReceptor("64000");
        receptor.setRegimenFiscalReceptor("616");

        comprobante.setReceptor(receptor);

        // CONCEPTO
        Comprobante.Conceptos conceptos = new Comprobante.Conceptos();
        Comprobante.Conceptos.Concepto concepto = new Comprobante.Conceptos.Concepto();

        concepto.setClaveProdServ("86121500");
        concepto.setCantidad(new BigDecimal("1"));
        concepto.setClaveUnidad("E48");
        concepto.setDescripcion("Pago de colegiatura");
        concepto.setValorUnitario(new BigDecimal("100.00"));
        concepto.setImporte(new BigDecimal("100.00"));

        conceptos.getConcepto().add(concepto);
        comprobante.setConceptos(conceptos);

        // JAXB
        JAXBContext context = JAXBContext.newInstance(Comprobante.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        // 1 GENERAR XML SIN FIRMA
        marshaller.marshal(comprobante, new File("cfdi.xml"));

        // 2 GENERAR CADENA ORIGINAL
        String cadena = CfdiCadenaOriginalGenerator.generarCadena(
                "cfdi.xml",
                "src/main/resources/xslt/cadenaoriginal_4_0.xslt"
        );

        System.out.println("Cadena Original:");
        System.out.println(cadena);

        // 3 FIRMAR
        String keyPath = "src/main/resources/csd/prueba.key";
        String cerPath = "src/main/resources/csd/prueba.cer";

        String sello = CfdiSigner.generarSello(cadena, keyPath);
        String certificado = CfdiSigner.getCertificadoBase64(cerPath);
        String noCertificado = CfdiSigner.getNoCertificado(cerPath);

        // 4 AGREGAR FIRMA
        comprobante.setSello(sello);
        comprobante.setCertificado(certificado);
        comprobante.setNoCertificado(noCertificado);

        // 5 GENERAR XML FIRMADO
        marshaller.marshal(comprobante, new File("cfdi_firmado.xml"));
        marshaller.marshal(comprobante, System.out);

        // 6 VALIDAR
        CfdiValidator.validate(
                "cfdi_firmado.xml",
                "src/main/resources/xsd/cfdv40.xsd"
        );
    }
}