package mx.cdefis.cfdi;

import mx.cdefis.cfdi.model.*;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.GregorianCalendar;

public class GeneradorIEDU {

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

        // =====================
        // BASE
        // =====================
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
        receptor.setRfc("CUSC850516316");
        receptor.setNombre("CESAR OSBALDO CRUZ SOLORZANO");
        receptor.setUsoCFDI(CUsoCFDI.D_10);
        receptor.setDomicilioFiscalReceptor("45638");
        receptor.setRegimenFiscalReceptor("605");
        comprobante.setReceptor(receptor);

        // =====================
        // CONCEPTO
        // =====================
        Comprobante.Conceptos conceptos = new Comprobante.Conceptos();
        Comprobante.Conceptos.Concepto concepto = new Comprobante.Conceptos.Concepto();

        concepto.setClaveProdServ("86121500");
        concepto.setCantidad(BigDecimal.ONE);
        concepto.setClaveUnidad("E48");
        concepto.setDescripcion("Colegiatura bachillerato");
        concepto.setValorUnitario(base);
        concepto.setImporte(base);

        // =====================
        // COMPLEMENTO IEDU
        // =====================
        InstEducativas iedu = new InstEducativas();
        iedu.setNombreAlumno("JUAN PEREZ LOPEZ");
        iedu.setCurp("PELO800101HDFRRL09");
        iedu.setNivelEducativo("Bachillerato o su equivalente");
        iedu.setAutRVOE("123456");

        Comprobante.Conceptos.Concepto.ComplementoConcepto complemento =
                new Comprobante.Conceptos.Concepto.ComplementoConcepto();

        complemento.getAny().add(iedu);
        concepto.setComplementoConcepto(complemento);

        // =====================
        // LOGICA IVA / NO IVA
        // =====================
        if (conIVA) {

            // OBJETO IMPUESTO
            concepto.setObjetoImp("02");

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

            // ===== IMPUESTOS GLOBAL =====
            Comprobante.Impuestos impuestos = new Comprobante.Impuestos();

            Comprobante.Impuestos.Traslados traslados =
                    new Comprobante.Impuestos.Traslados();

            Comprobante.Impuestos.Traslados.Traslado traslado =
                    new Comprobante.Impuestos.Traslados.Traslado();

            traslado.setBase(base);
            traslado.setImpuesto("002");
            traslado.setTipoFactor(CTipoFactor.TASA);
            traslado.setTasaOCuota(new BigDecimal("0.160000"));
            traslado.setImporte(iva);

            traslados.getTraslado().add(traslado);
            impuestos.setTraslados(traslados);
            impuestos.setTotalImpuestosTrasladados(iva);

            comprobante.setImpuestos(impuestos);

            comprobante.setTotal(base.add(iva));

        } else {

            // SIN IVA (EXENTO / NO OBJETO)
            concepto.setObjetoImp("01");

            comprobante.setTotal(base);

            // NO impuestos en ningún nivel
        }

        conceptos.getConcepto().add(concepto);
        comprobante.setConceptos(conceptos);

        return comprobante;
    }
}