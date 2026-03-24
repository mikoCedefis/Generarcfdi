package mx.cdefis.cfdi.timbrado;

import mx.cdefis.cfdi.timbrado.soap.*;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.namespace.QName;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class FinkokTimbrador {

    private final Application port;

    public FinkokTimbrador() {
        StampSOAP service = new StampSOAP();
        this.port = service.getApplication();

        installLoggingHandler(this.port);

        // Timeouts (MUY importante en producción)
        BindingProvider bp = (BindingProvider) this.port;
        bp.getRequestContext().put("com.sun.xml.ws.connect.timeout", 10000);
        bp.getRequestContext().put("com.sun.xml.ws.request.timeout", 20000);
    }

    // Método principal
    public String timbrar(String xml, String username, String password) {
        try {
            byte[] xmlBytes = xml.getBytes(StandardCharsets.UTF_8);

            AcuseRecepcionCFDI acuse = port.stamp(
                    xmlBytes,
                    username,
                    password
            );

            String codEstatus = get(acuse.getCodEstatus());
            String uuid = get(acuse.getUUID());

            JAXBElement<String> xmlJ = acuse.getXml();
            String xmlBase64 = (xmlJ != null) ? xmlJ.getValue() : null;

            // Validación mínima (sin duplicar lógica del PAC)
            if (xmlBase64 == null || xmlBase64.trim().isEmpty()) {
                throw new RuntimeException(
                        "El PAC no devolvió XML timbrado. CodEstatus=" + codEstatus + " UUID=" + uuid
                );
            }

            String xmlTimbrado;

            if (xmlBase64.trim().startsWith("<")) {
                // Ya es XML plano
                xmlTimbrado = xmlBase64;
                System.out.println("[timbrar] XML recibido directamente (no Base64)");
            } else {
                // Es Base64
                xmlTimbrado = new String(
                        Base64.getDecoder().decode(xmlBase64),
                        StandardCharsets.UTF_8
                );
                System.out.println("[timbrar] XML decodificado desde Base64");
            }

            System.out.println("✔ CFDI timbrado correctamente - UUID: " + uuid);

            return xmlTimbrado;

        } catch (Exception e) {
            throw new RuntimeException("Error al timbrar CFDI con Finkok", e);
        }
    }

    // Metodo opcional usando variables de entorno (util para producción)
    public String timbrar(String xml) {

        Dotenv dotenv = Dotenv.configure()
                .directory("./") // raíz del proyecto
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        String username = dotenv.get("FINKOK_USER");
        String password = dotenv.get("FINKOK_PASS");

//        System.out.println("USER: " + username);
//        System.out.println("PASS: " + password);

        if (username == null || password == null) {
            throw new RuntimeException("Credenciales FINKOK no configuradas en variables de entorno");
        }

        return timbrar(xml, username, password);
    }

    // Helper para simplificar JAXBElement
    private String get(JAXBElement<String> el) {
        return el != null ? el.getValue() : null;
    }

    // =========================
    // SOAP LOGGING HANDLER
    // =========================
    private void installLoggingHandler(Application port) {
        try {
            BindingProvider bp = (BindingProvider) port;
            Binding binding = bp.getBinding();

            List<Handler> handlerChain = binding.getHandlerChain();
            if (handlerChain == null) {
                handlerChain = new ArrayList<>();
            }

            handlerChain.add(new SOAPLoggingHandler());
            binding.setHandlerChain(handlerChain);

        } catch (Exception ex) {
            System.out.println("[SOAP LOG] No se pudo instalar handler: " + ex.getMessage());
        }
    }

    private static class SOAPLoggingHandler implements SOAPHandler<SOAPMessageContext> {

        private static final int MAX_LOG_SIZE = 2000;

        @Override
        public boolean handleMessage(SOAPMessageContext context) {
            Boolean outbound = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                context.getMessage().writeTo(baos);

                String msg = baos.toString(StandardCharsets.UTF_8);

                // =========================
                // CONSOLA (SIEMPRE)
                // =========================
                if (outbound) {
                    System.out.println("\n[SOAP REQUEST]\n" + msg);
                } else {
                    System.out.println("\n[SOAP RESPONSE]\n" + msg);
                }

                // =========================
                // ARCHIVO (SIEMPRE)
                // =========================
                try {
                    String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                            .withZone(java.time.ZoneId.systemDefault())
                            .format(Instant.now());

                    Path dir = Path.of("logs");

                    // Crear directorio si no existe
                    if (!Files.exists(dir)) {
                        Files.createDirectories(dir);
                    }

                    Path file;

                    if (outbound) {
                        file = Path.of("logs/soap_request_" + ts + ".xml");
                    } else {
                        file = Path.of("logs/soap_response_" + ts + ".xml");
                    }

                    Files.writeString(
                            file,
                            msg,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    );

                    System.out.println("[SOAP LOG] Guardado en: " + file.toAbsolutePath());

                } catch (Exception e) {
                    System.out.println("[SOAP LOG] Error al guardar archivo: " + e.getMessage());
                }

            } catch (Exception e) {
                System.out.println("[SOAP LOG] Error procesando mensaje: " + e.getMessage());
            }

            return true;
        }

        @Override
        public boolean handleFault(SOAPMessageContext context) {
            return handleMessage(context);
        }

        @Override
        public void close(MessageContext context) {
        }

        @Override
        public Set<QName> getHeaders() {
            return null;
        }
    }
}