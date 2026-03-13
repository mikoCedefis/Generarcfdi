package mx.cdefis.cfdi;

import mx.cdefis.cfdi.model.Comprobante;

public class Main {

    public static void main(String[] args) {
        try {

            // IEDU SIN IVA (lo más común)
            //Comprobante comprobante = GeneradorIEDU.generar(false);

            // IEDU CON IVA (si lo necesitas)
            // Comprobante comprobante = GeneradorIEDU.generar(true);

            // INGRESO NORMAL CON IVA
             Comprobante comprobante = GeneradorIngresoGeneral.generar(false);

            // INGRESO SIN IVA
            // Comprobante comprobante = GeneradorIngresoGeneral.generar(false);


            // =========================
            // PROCESAR (FIRMAR + XML + VALIDAR)
            // =========================
            CfdiProcessor.process(comprobante, null);

            System.out.println("✅ CFDI generado correctamente");

        } catch (Exception e) {
            System.out.println("❌ Error al generar CFDI:");
            e.printStackTrace();
        }
    }
}