package mx.cdefis.cfdi.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class XmlUtils {

    public static String extraerUUID(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // 🔥 CLAVE: activar namespaces
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            // Buscar por namespace correcto del SAT
            NodeList nodes = doc.getElementsByTagNameNS(
                    "http://www.sat.gob.mx/TimbreFiscalDigital",
                    "TimbreFiscalDigital"
            );

            if (nodes.getLength() == 0) {
                throw new RuntimeException("No se encontró el nodo TimbreFiscalDigital en el XML");
            }

            Element tfd = (Element) nodes.item(0);

            String uuid = tfd.getAttribute("UUID");

            if (uuid == null || uuid.isEmpty()) {
                throw new RuntimeException("El nodo TimbreFiscalDigital no contiene UUID");
            }

            return uuid;

        } catch (Exception e) {
            throw new RuntimeException("Error al extraer el UUID del XML", e);
        }
    }
}
