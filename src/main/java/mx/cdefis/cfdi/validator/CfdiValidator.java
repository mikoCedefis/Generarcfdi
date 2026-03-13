package mx.cdefis.cfdi.validator;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.File;

public class CfdiValidator {

    public static void validate(String xmlPath, String xsdPath) {
        try {

            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Schema schema = factory.newSchema(new File(xsdPath));

            Validator validator = schema.newValidator();

            validator.validate(new StreamSource(new File(xmlPath)));

            System.out.println("CFDI válido contra el XSD");

        } catch (Exception e) {
            System.out.println("Error de validación:");
            System.out.println(e.getMessage());
        }
    }
}