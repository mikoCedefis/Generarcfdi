package mx.cdefis.cfdi;

import mx.cdefis.cfdi.model.*;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConstants;

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import java.math.RoundingMode;

public class GeneradorIngresoGeneral {

    public static Comprobante generar(boolean conIVA) throws Exception {

        Comprobante comprobante = new Comprobante();

        // =====================
        // FECHA
        // =====================
        GregorianCalendar cal = new GregorianCalendar();

        XMLGregorianCalendar fecha =
                DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);

        fecha.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        fecha.setTimezone(DatatypeConstants.FIELD_UNDEFINED);

        // Base global
        BigDecimal base = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP);


        // =====================
        // DATOS CFDI
        // =====================
        comprobante.setVersion("4.0");
        comprobante.setTipoDeComprobante(CTipoDeComprobante.I);
        comprobante.setFecha(fecha);
        comprobante.setSubTotal(base);
        comprobante.setMoneda(CMoneda.MXN);
        comprobante.setLugarExpedicion("58260");
        comprobante.setExportacion("01");
        comprobante.setMetodoPago(CMetodoPago.PUE);
        comprobante.setFormaPago("01");

        // =====================
        // EMISOR
        // =====================
        Comprobante.Emisor emisor = new Comprobante.Emisor();
        emisor.setRfc("EKU9003173C9");
        emisor.setNombre("ESCUELA KEMPER URGATE");
        emisor.setRegimenFiscal("601");

        comprobante.setEmisor(emisor);

        // =====================
        // RECEPTOR
        // =====================
        Comprobante.Receptor receptor = new Comprobante.Receptor();
        receptor.setRfc("XAXX010101000");
        receptor.setNombre("PUBLICO GENERAL");
        receptor.setUsoCFDI(CUsoCFDI.S_01);
        receptor.setDomicilioFiscalReceptor("58260");
        receptor.setRegimenFiscalReceptor("616");

        comprobante.setReceptor(receptor);

        // =====================
        // CONCEPTO
        // =====================
        Comprobante.Conceptos conceptos = new Comprobante.Conceptos();
        Comprobante.Conceptos.Concepto concepto = new Comprobante.Conceptos.Concepto();

        // 👉 CAMBIA SEGÚN CASO
        concepto.setClaveProdServ("53102700"); // uniforme
        concepto.setCantidad(new BigDecimal("1"));
        concepto.setClaveUnidad("H87");
        concepto.setDescripcion("Venta de uniforme escolar");
        concepto.setValorUnitario(base);
        concepto.setImporte(base);

        if (conIVA) {
            concepto.setObjetoImp("02");
        } else {
            concepto.setObjetoImp("01");
        }

        // =====================
        // IMPUESTOS
        // =====================
        if (conIVA) {

            BigDecimal tasa = new BigDecimal("0.16");

            BigDecimal iva = base.multiply(tasa).setScale(2, RoundingMode.HALF_UP);

            // CONCEPTO
            Comprobante.Conceptos.Concepto.Impuestos impConcepto =
                    new Comprobante.Conceptos.Concepto.Impuestos();

            Comprobante.Conceptos.Concepto.Impuestos.Traslados traslados =
                    new Comprobante.Conceptos.Concepto.Impuestos.Traslados();

            Comprobante.Conceptos.Concepto.Impuestos.Traslados.Traslado traslado =
                    new Comprobante.Conceptos.Concepto.Impuestos.Traslados.Traslado();

            traslado.setBase(base);
            traslado.setImpuesto("002");
            traslado.setTipoFactor(CTipoFactor.TASA);
            traslado.setTasaOCuota(new BigDecimal("0.160000"));
            traslado.setImporte(iva);

            traslados.getTraslado().add(traslado);
            impConcepto.setTraslados(traslados);

            concepto.setImpuestos(impConcepto);

            // NIVEL COMPROBANTE
            Comprobante.Impuestos impuestos = new Comprobante.Impuestos();

            Comprobante.Impuestos.Traslados trasladosComp =
                    new Comprobante.Impuestos.Traslados();

            Comprobante.Impuestos.Traslados.Traslado trasladoComp =
                    new Comprobante.Impuestos.Traslados.Traslado();

            trasladoComp.setBase(base);
            trasladoComp.setImpuesto("002");
            trasladoComp.setTipoFactor(CTipoFactor.TASA);
            trasladoComp.setTasaOCuota(new BigDecimal("0.160000"));
            trasladoComp.setImporte(iva);

            trasladosComp.getTraslado().add(trasladoComp);
            impuestos.setTraslados(trasladosComp);

            impuestos.setTotalImpuestosTrasladados(iva);

            comprobante.setImpuestos(impuestos);

            comprobante.setTotal(
                    base.add(iva).setScale(2, RoundingMode.HALF_UP)
            );

        } else {
            // SIN IMPUESTOS
            comprobante.setTotal(new BigDecimal("100.00"));
        }

        conceptos.getConcepto().add(concepto);
        comprobante.setConceptos(conceptos);

        return comprobante;
    }
}