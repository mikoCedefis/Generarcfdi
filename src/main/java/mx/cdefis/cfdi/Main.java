package mx.cdefis.cfdi;

import mx.cdefis.cfdi.builder.CfdiBuilder;
import mx.cdefis.cfdi.builder.GeneradorIngresoGeneral;
import mx.cdefis.cfdi.builder.GeneradorIEDU;
import mx.cdefis.cfdi.model.Comprobante;

public class Main {

    public static void main(String[] args) {
        try {

            // Generar Ingresos
           CfdiBuilder builder = new GeneradorIngresoGeneral(false);

           CfdiProcessor.process(builder, false);

            // Generar IEDU
            //CfdiBuilder builder = new GeneradorIEDU(true);

            //CfdiProcessor.process(builder, true);


        } catch (Exception e) {
            System.out.println("❌ Error al generar CFDI:");
            e.printStackTrace();
        }
    }
}