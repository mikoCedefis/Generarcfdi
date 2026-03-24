package mx.cdefis.cfdi.builder;

import mx.cdefis.cfdi.model.Comprobante;

public interface CfdiBuilder {

    Comprobante build() throws Exception;
}