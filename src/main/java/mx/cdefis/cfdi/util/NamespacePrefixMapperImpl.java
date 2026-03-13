package mx.cdefis.cfdi.util;

import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

public class NamespacePrefixMapperImpl extends NamespacePrefixMapper {

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {

        if ("http://www.sat.gob.mx/cfd/4".equals(namespaceUri)) {
            return "cfdi";
        }

        if ("http://www.sat.gob.mx/iedu".equals(namespaceUri)) {
            return "iedu";
        }

        if ("http://www.w3.org/2001/XMLSchema-instance".equals(namespaceUri)) {
            return "xsi";
        }

        return suggestion;
    }
}