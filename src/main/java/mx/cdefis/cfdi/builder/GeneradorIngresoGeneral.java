package mx.cdefis.cfdi.builder;

import mx.cdefis.cfdi.model.*;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.GregorianCalendar;

public class GeneradorIngresoGeneral implements CfdiBuilder {

    private final boolean conIVA;

    public GeneradorIngresoGeneral(boolean conIVA) {
        this.conIVA = conIVA;
    }

    @Override
    public Comprobante build() throws Exception {

        Comprobante comprobante = new Comprobante();

        // =====================
        // FECHA
        // =====================
        comprobante.setFecha(crearFecha());

        BigDecimal base = new BigDecimal("100.00").setScale(2, RoundingMode.HALF_UP);

        // =====================
        // DATOS CFDI
        // =====================
        comprobante.setVersion("4.0");
        comprobante.setTipoDeComprobante(CTipoDeComprobante.I);
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
        //receptor.setUsoCFDI(CUsoCFDI.D_10);
        receptor.setDomicilioFiscalReceptor("58260");
        receptor.setRegimenFiscalReceptor("616");
        comprobante.setReceptor(receptor);

        // =====================
        // CONCEPTO
        // =====================
        Comprobante.Conceptos conceptos = new Comprobante.Conceptos();
        Comprobante.Conceptos.Concepto concepto = new Comprobante.Conceptos.Concepto();

        concepto.setClaveProdServ("53102700");
        concepto.setCantidad(BigDecimal.ONE);
        concepto.setClaveUnidad("H87");
        concepto.setDescripcion("Venta de uniforme escolar");
        concepto.setValorUnitario(base);
        concepto.setImporte(base);
        concepto.setObjetoImp(conIVA ? "02" : "01");

        // =====================
        // IMPUESTOS
        // =====================
        if (conIVA) {

            BigDecimal tasa = new BigDecimal("0.16");
            BigDecimal iva = base.multiply(tasa).setScale(2, RoundingMode.HALF_UP);

            // ===== IMPUESTOS CONCEPTO =====
            Comprobante.Conceptos.Concepto.Impuestos impConcepto =
                    new Comprobante.Conceptos.Concepto.Impuestos();

            Comprobante.Conceptos.Concepto.Impuestos.Traslados trasladosConcepto =
                    new Comprobante.Conceptos.Concepto.Impuestos.Traslados();

            Comprobante.Conceptos.Concepto.Impuestos.Traslados.Traslado trasladoConcepto =
                    new Comprobante.Conceptos.Concepto.Impuestos.Traslados.Traslado();

            trasladoConcepto.setBase(base);
            trasladoConcepto.setImpuesto("002");
            trasladoConcepto.setTipoFactor(CTipoFactor.TASA);
            trasladoConcepto.setTasaOCuota(new BigDecimal("0.160000"));
            trasladoConcepto.setImporte(iva);

            trasladosConcepto.getTraslado().add(trasladoConcepto);
            impConcepto.setTraslados(trasladosConcepto);
            concepto.setImpuestos(impConcepto);

            // ===== IMPUESTOS COMPROBANTE =====
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
            comprobante.setTotal(base);
        }

        conceptos.getConcepto().add(concepto);
        comprobante.setConceptos(conceptos);

        return comprobante;
    }

    // =====================
    // UTILIDAD FECHA
    // =====================
    private XMLGregorianCalendar crearFecha() throws Exception {
        GregorianCalendar cal = new GregorianCalendar();

        XMLGregorianCalendar fecha =
                DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);

        fecha.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        fecha.setTimezone(DatatypeConstants.FIELD_UNDEFINED);

        return fecha;
    }
}