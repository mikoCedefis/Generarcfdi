package mx.cdefis.cfdi.cadena;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringWriter;

public class CfdiCadenaOriginalGenerator {

    public static String generarCadena(String xmlPath, String xsltPath) throws Exception {

        TransformerFactory factory = TransformerFactory.newInstance();

        StreamSource xslt = new StreamSource(xsltPath);
        Transformer transformer = factory.newTransformer(xslt);

        StreamSource xml = new StreamSource(xmlPath);

        StringWriter writer = new StringWriter();

        transformer.transform(xml, new StreamResult(writer));

        return writer.toString().trim();
    }
}
