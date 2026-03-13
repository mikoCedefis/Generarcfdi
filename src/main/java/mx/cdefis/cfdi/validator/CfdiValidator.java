package mx.cdefis.cfdi.validator;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.File;

public class CfdiValidator {

    public static void validate(String xmlPath, String... xsdPaths) {
        try {

            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // cargar multiples xsd
            Source[] schemas = new Source[xsdPaths.length];

            for (int i = 0; i < xsdPaths.length; i++) {
                schemas[i] = new StreamSource(new File(xsdPaths[i]));
            }

            Schema schema = factory.newSchema(schemas);

            Validator validator = schema.newValidator();

            validator.validate(new StreamSource(new File(xmlPath)));

            System.out.println("CFDI válido contra el XSD");

        } catch (Exception e) {
            System.out.println("Error de validación:");
            System.out.println(e.getMessage());
        }
    }
}