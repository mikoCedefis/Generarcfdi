# Migración a Spring Boot con Arquitectura Hexagonal

Esta guía describe paso a paso cómo adaptar el proyecto `generar_cfdi` a una aplicación
Spring Boot usando arquitectura hexagonal (puertos y adaptadores).

---

## Tabla de contenido

1. [¿Por qué arquitectura hexagonal?](#1-por-qué-arquitectura-hexagonal)
2. [Estructura de paquetes objetivo](#2-estructura-de-paquetes-objetivo)
3. [Mapa de clases: origen → destino](#3-mapa-de-clases-origen--destino)
4. [Paso 1: Crear el proyecto Spring Boot](#paso-1-crear-el-proyecto-spring-boot)
5. [Paso 2: Definir los puertos (interfaces de dominio)](#paso-2-definir-los-puertos-interfaces-de-dominio)
6. [Paso 3: Mover los modelos de dominio](#paso-3-mover-los-modelos-de-dominio)
7. [Paso 4: Implementar los servicios de dominio](#paso-4-implementar-los-servicios-de-dominio)
8. [Paso 5: Crear los adaptadores de infraestructura](#paso-5-crear-los-adaptadores-de-infraestructura)
9. [Paso 6: Crear el controlador REST](#paso-6-crear-el-controlador-rest)
10. [Paso 7: Configuración de Spring](#paso-7-configuración-de-spring)
11. [Paso 8: Migrar los recursos estáticos](#paso-8-migrar-los-recursos-estáticos)
12. [Paso 9: Ajustar el pom.xml](#paso-9-ajustar-el-pomxml)
13. [Problemas conocidos y cómo resolverlos](#problemas-conocidos-y-cómo-resolverlos)

---

## 1. ¿Por qué arquitectura hexagonal?

El proyecto actual tiene todo acoplado en clases estáticas con rutas hardcodeadas.
La arquitectura hexagonal separa en tres capas:

```
┌─────────────────────────────────────────────┐
│              INFRAESTRUCTURA                │  ← Spring, REST, JAXB, BouncyCastle
│   ┌─────────────────────────────────────┐   │
│   │           APLICACIÓN                │   │  ← DTOs, casos de uso
│   │   ┌─────────────────────────────┐   │   │
│   │   │          DOMINIO            │   │   │  ← Lógica de negocio pura
│   │   │  (sin dependencias externas)│   │   │
│   │   └─────────────────────────────┘   │   │
│   └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

**Beneficios concretos para este proyecto:**
- El dominio (construcción del CFDI) queda independiente de Spring
- Los certificados/rutas se inyectan via configuración, no hardcodeados
- Se pueden hacer tests unitarios del dominio sin levantar el contexto Spring
- Se puede cambiar JAXB por otra librería XML sin tocar la lógica de negocio

---

## 2. Estructura de paquetes objetivo

```
mx.cdefis.cfdi/
│
├── domain/
│   ├── model/
│   │   ├── CfdiIeduRequest.java         ← Datos para generar CFDI IEDU
│   │   └── CfdiIngresoRequest.java      ← Datos para generar CFDI Ingreso
│   ├── port/
│   │   ├── in/
│   │   │   ├── GenerarCfdiIeduUseCase.java
│   │   │   └── GenerarCfdiIngresoUseCase.java
│   │   └── out/
│   │       ├── CfdiXmlPort.java         ← Serializar/deserializar XML
│   │       ├── CfdiSignerPort.java      ← Firmar y obtener certificado
│   │       ├── CfdiCadenaPort.java      ← Generar cadena original
│   │       └── CfdiValidatorPort.java   ← Validar contra XSD
│   └── service/
│       ├── CfdiIeduService.java         ← Implementa GenerarCfdiIeduUseCase
│       └── CfdiIngresoService.java      ← Implementa GenerarCfdiIngresoUseCase
│
├── application/
│   └── dto/
│       ├── CfdiIeduRequestDto.java      ← Input del API REST
│       ├── CfdiIngresoRequestDto.java
│       └── CfdiResponseDto.java         ← Output del API REST (XML firmado)
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   └── CfdiController.java      ← @RestController
    │   └── out/
    │       ├── CfdiXmlAdapter.java      ← JAXB (implementa CfdiXmlPort)
    │       ├── CfdiSignerAdapter.java   ← BouncyCastle (implementa CfdiSignerPort)
    │       ├── CfdiCadenaAdapter.java   ← XSLT (implementa CfdiCadenaPort)
    │       └── CfdiValidatorAdapter.java← XSD (implementa CfdiValidatorPort)
    └── config/
        └── CfdiProperties.java          ← @ConfigurationProperties
```

---

## 3. Mapa de clases: origen → destino

| Clase original | Destino en Spring Boot | Tipo de cambio |
|---|---|---|
| `GeneradorIEDU.java` | `domain/service/CfdiIeduService.java` | Refactorizar: extraer lógica de construcción |
| `GeneradorIngresoGeneral.java` | `domain/service/CfdiIngresoService.java` | Igual |
| `CfdiProcessor.java` | Se **elimina** como clase única | Dividir en adaptadores + servicio |
| `CfdiSigner.java` | `infrastructure/adapter/out/CfdiSignerAdapter.java` | `@Component` + implementar interfaz |
| `CfdiValidator.java` | `infrastructure/adapter/out/CfdiValidatorAdapter.java` | `@Component` + implementar interfaz |
| `CfdiCadenaOriginalGenerator.java` | `infrastructure/adapter/out/CfdiCadenaAdapter.java` | `@Component` + implementar interfaz |
| `InstEducativas.java` | `domain/model/` | Sin cambios |
| `NamespacePrefixMapperImpl.java` | `infrastructure/adapter/out/CfdiXmlAdapter.java` | Mover como clase interna o inner class |
| `Comprobante.java` (generada) | Igual, en `model/` generado por JAXB | Sin cambios |

---

## Paso 1: Crear el proyecto Spring Boot

### Opción A: Spring Initializr

Ve a [start.spring.io](https://start.spring.io) y configura:

| Campo | Valor |
|-------|-------|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.3.x |
| Group | `mx.cdefis` |
| Artifact | `cfdi-service` |
| Java | 21 |
| Dependencies | Spring Web |

### Opción B: Agregar al pom.xml existente

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Mantener las dependencias existentes: JAXB + BouncyCastle -->
</dependencies>
```

---

## Paso 2: Definir los puertos (interfaces de dominio)

Estos son contratos que el dominio define y la infraestructura implementa.

### Puerto de entrada (caso de uso)

```java
// domain/port/in/GenerarCfdiIeduUseCase.java
package mx.cdefis.cfdi.domain.port.in;

import mx.cdefis.cfdi.domain.model.CfdiIeduRequest;

public interface GenerarCfdiIeduUseCase {
    String ejecutar(CfdiIeduRequest request) throws Exception;
}
```

```java
// domain/port/in/GenerarCfdiIngresoUseCase.java
package mx.cdefis.cfdi.domain.port.in;

import mx.cdefis.cfdi.domain.model.CfdiIngresoRequest;

public interface GenerarCfdiIngresoUseCase {
    String ejecutar(CfdiIngresoRequest request) throws Exception;
}
```

### Puertos de salida (infraestructura)

```java
// domain/port/out/CfdiSignerPort.java
package mx.cdefis.cfdi.domain.port.out;

public interface CfdiSignerPort {
    String generarSello(String cadenaOriginal) throws Exception;
    String getCertificadoBase64();
    String getNoCertificado();
}
```

```java
// domain/port/out/CfdiCadenaPort.java
package mx.cdefis.cfdi.domain.port.out;

public interface CfdiCadenaPort {
    String generarCadena(String xmlContent) throws Exception;
}
```

```java
// domain/port/out/CfdiXmlPort.java
package mx.cdefis.cfdi.domain.port.out;

import mx.cdefis.cfdi.model.Comprobante;

public interface CfdiXmlPort {
    String toXml(Comprobante comprobante) throws Exception;
}
```

```java
// domain/port/out/CfdiValidatorPort.java
package mx.cdefis.cfdi.domain.port.out;

public interface CfdiValidatorPort {
    void validar(String xmlContent, String... xsdPaths);
}
```

---

## Paso 3: Mover los modelos de dominio

Crea clases POJO que representen la entrada del caso de uso (no el `Comprobante` de JAXB):

```java
// domain/model/CfdiIeduRequest.java
package mx.cdefis.cfdi.domain.model;

public class CfdiIeduRequest {
    private String rfcEmisor;
    private String nombreEmisor;
    private String regimenFiscal;
    private String rfcReceptor;
    private String nombreReceptor;
    private String domicilioFiscalReceptor;
    private String regimenFiscalReceptor;
    private String lugarExpedicion;
    private String nombreAlumno;
    private String curp;
    private String nivelEducativo;
    private String autRVOE;
    private String descripcionConcepto;
    private java.math.BigDecimal importe;
    private boolean conIVA;

    // getters y setters...
}
```

```java
// domain/model/CfdiIngresoRequest.java
package mx.cdefis.cfdi.domain.model;

public class CfdiIngresoRequest {
    private String rfcEmisor;
    private String nombreEmisor;
    private String regimenFiscal;
    private String rfcReceptor;
    private String nombreReceptor;
    private String domicilioFiscalReceptor;
    private String regimenFiscalReceptor;
    private String lugarExpedicion;
    private String claveProdServ;
    private String claveUnidad;
    private String descripcionConcepto;
    private java.math.BigDecimal importe;
    private boolean conIVA;

    // getters y setters...
}
```

---

## Paso 4: Implementar los servicios de dominio

El servicio recibe el request, construye el `Comprobante`, y llama a los puertos de salida.

```java
// domain/service/CfdiIeduService.java
package mx.cdefis.cfdi.domain.service;

import mx.cdefis.cfdi.domain.model.CfdiIeduRequest;
import mx.cdefis.cfdi.domain.port.in.GenerarCfdiIeduUseCase;
import mx.cdefis.cfdi.domain.port.out.*;
import mx.cdefis.cfdi.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CfdiIeduService implements GenerarCfdiIeduUseCase {

    private final CfdiSignerPort signer;
    private final CfdiCadenaPort cadena;
    private final CfdiXmlPort xml;
    private final CfdiValidatorPort validator;

    // Inyección por constructor (recomendado)
    public CfdiIeduService(CfdiSignerPort signer,
                           CfdiCadenaPort cadena,
                           CfdiXmlPort xml,
                           CfdiValidatorPort validator) {
        this.signer = signer;
        this.cadena = cadena;
        this.xml = xml;
        this.validator = validator;
    }

    @Override
    public String ejecutar(CfdiIeduRequest request) throws Exception {

        // 1. Construir el Comprobante (lógica extraída de GeneradorIEDU)
        Comprobante comprobante = construirComprobante(request);

        // 2. Agregar certificado
        comprobante.setCertificado(signer.getCertificadoBase64());
        comprobante.setNoCertificado(signer.getNoCertificado());

        // 3. Serializar a XML sin sello
        String xmlSinSello = xml.toXml(comprobante);

        // 4. Generar cadena original
        String cadenaOriginal = cadena.generarCadena(xmlSinSello);

        // 5. Firmar
        String sello = signer.generarSello(cadenaOriginal);
        comprobante.setSello(sello);

        // 6. Serializar a XML final con sello
        String xmlFirmado = xml.toXml(comprobante);

        // 7. Validar
        validator.validar(xmlFirmado, "cfdv40.xsd", "iedu.xsd");

        return xmlFirmado;
    }

    private Comprobante construirComprobante(CfdiIeduRequest request) {
        // Mover aquí la lógica de GeneradorIEDU.generar()
        // usando request.getXxx() en lugar de valores hardcodeados
        // ...
        return comprobante;
    }
}
```

---

## Paso 5: Crear los adaptadores de infraestructura

### Adaptador de firma (`CfdiSigner.java` → `CfdiSignerAdapter.java`)

```java
// infrastructure/adapter/out/CfdiSignerAdapter.java
package mx.cdefis.cfdi.infrastructure.adapter.out;

import mx.cdefis.cfdi.domain.port.out.CfdiSignerPort;
import mx.cdefis.cfdi.infrastructure.config.CfdiProperties;
import org.springframework.stereotype.Component;

@Component
public class CfdiSignerAdapter implements CfdiSignerPort {

    private final CfdiProperties props;

    public CfdiSignerAdapter(CfdiProperties props) {
        this.props = props;
    }

    @Override
    public String generarSello(String cadenaOriginal) throws Exception {
        // Copiar lógica de CfdiSigner.generarSello()
        // Leer keyPath desde props.getKeyPath()
        // Leer password desde props.getKeyPassword()
        PrivateKey key = getPrivateKey(props.getKeyPath(), props.getKeyPassword());
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update(cadenaOriginal.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    @Override
    public String getCertificadoBase64() {
        // Copiar lógica de CfdiSigner.getCertificadoBase64()
        // Usar props.getCerName()
    }

    @Override
    public String getNoCertificado() {
        // Copiar lógica de CfdiSigner.getNoCertificado()
    }

    private PrivateKey getPrivateKey(String keyPath, String password) throws Exception {
        // Copiar lógica sin cambios de CfdiSigner.getPrivateKey()
    }
}
```

### Adaptador XML (`CfdiProcessor` + `NamespacePrefixMapperImpl` → `CfdiXmlAdapter.java`)

```java
// infrastructure/adapter/out/CfdiXmlAdapter.java
package mx.cdefis.cfdi.infrastructure.adapter.out;

import mx.cdefis.cfdi.domain.port.out.CfdiXmlPort;
import mx.cdefis.cfdi.model.Comprobante;
import mx.cdefis.cfdi.model.InstEducativas;
import mx.cdefis.cfdi.util.NamespacePrefixMapperImpl;
import org.springframework.stereotype.Component;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;

@Component
public class CfdiXmlAdapter implements CfdiXmlPort {

    private final JAXBContext jaxbContext;

    public CfdiXmlAdapter() throws Exception {
        this.jaxbContext = JAXBContext.newInstance(Comprobante.class, InstEducativas.class);
    }

    @Override
    public String toXml(Comprobante comprobante) throws Exception {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper",
                new NamespacePrefixMapperImpl());
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                "http://www.sat.gob.mx/cfd/4 " +
                "http://www.sat.gob.mx/sitio_internet/cfd/4/cfdv40.xsd " +
                "http://www.sat.gob.mx/iedu " +
                "http://www.sat.gob.mx/sitio_internet/cfd/iedu/iedu.xsd");

        StringWriter writer = new StringWriter();
        marshaller.marshal(comprobante, writer);
        return writer.toString();
    }
}
```

### Adaptador de cadena (`CfdiCadenaOriginalGenerator.java` → `CfdiCadenaAdapter.java`)

```java
// infrastructure/adapter/out/CfdiCadenaAdapter.java
package mx.cdefis.cfdi.infrastructure.adapter.out;

import mx.cdefis.cfdi.domain.port.out.CfdiCadenaPort;
import org.springframework.stereotype.Component;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.*;

@Component
public class CfdiCadenaAdapter implements CfdiCadenaPort {

    // XSLT cargado desde classpath al iniciar (no desde path relativo)
    private final Templates templates;

    public CfdiCadenaAdapter() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        InputStream xsltStream = getClass().getClassLoader()
                .getResourceAsStream("xslt/cadenaoriginal_4_0.xslt");
        this.templates = factory.newTemplates(new StreamSource(xsltStream));
    }

    @Override
    public String generarCadena(String xmlContent) throws Exception {
        Transformer transformer = templates.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(
                new StreamSource(new StringReader(xmlContent)),
                new StreamResult(writer)
        );
        return writer.toString().trim();
    }
}
```

> **Cambio importante:** En el proyecto original el XSLT se carga desde una ruta relativa
> del filesystem. En Spring Boot debe cargarse desde el `classpath:` usando
> `getClass().getClassLoader().getResourceAsStream(...)`.

### Adaptador de validación (`CfdiValidator.java` → `CfdiValidatorAdapter.java`)

```java
// infrastructure/adapter/out/CfdiValidatorAdapter.java
package mx.cdefis.cfdi.infrastructure.adapter.out;

import mx.cdefis.cfdi.domain.port.out.CfdiValidatorPort;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;
import java.io.StringReader;

@Component
public class CfdiValidatorAdapter implements CfdiValidatorPort {

    @Override
    public void validar(String xmlContent, String... xsdNames) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Source[] schemas = new Source[xsdNames.length];
            for (int i = 0; i < xsdNames.length; i++) {
                // Cargar desde classpath en lugar de File
                schemas[i] = new StreamSource(
                    getClass().getClassLoader().getResourceAsStream("xsd/" + xsdNames[i])
                );
            }

            Schema schema = factory.newSchema(schemas);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xmlContent)));

        } catch (Exception e) {
            throw new RuntimeException("CFDI inválido: " + e.getMessage(), e);
        }
    }
}
```

---

## Paso 6: Crear el controlador REST

```java
// infrastructure/adapter/in/rest/CfdiController.java
package mx.cdefis.cfdi.infrastructure.adapter.in.rest;

import mx.cdefis.cfdi.application.dto.*;
import mx.cdefis.cfdi.domain.port.in.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cfdi")
public class CfdiController {

    private final GenerarCfdiIeduUseCase cfdiIeduUseCase;
    private final GenerarCfdiIngresoUseCase cfdiIngresoUseCase;

    public CfdiController(GenerarCfdiIeduUseCase cfdiIeduUseCase,
                          GenerarCfdiIngresoUseCase cfdiIngresoUseCase) {
        this.cfdiIeduUseCase = cfdiIeduUseCase;
        this.cfdiIngresoUseCase = cfdiIngresoUseCase;
    }

    @PostMapping("/iedu")
    public ResponseEntity<CfdiResponseDto> generarIedu(
            @RequestBody CfdiIeduRequestDto dto) throws Exception {

        // Mapear DTO → modelo de dominio
        var request = mapIedu(dto);
        String xml = cfdiIeduUseCase.ejecutar(request);

        return ResponseEntity.ok(new CfdiResponseDto(xml));
    }

    @PostMapping("/ingreso")
    public ResponseEntity<CfdiResponseDto> generarIngreso(
            @RequestBody CfdiIngresoRequestDto dto) throws Exception {

        var request = mapIngreso(dto);
        String xml = cfdiIngresoUseCase.ejecutar(request);

        return ResponseEntity.ok(new CfdiResponseDto(xml));
    }
}
```

### DTOs de aplicación

```java
// application/dto/CfdiIeduRequestDto.java
public class CfdiIeduRequestDto {
    private String rfcEmisor;
    private String nombreEmisor;
    private String rfcReceptor;
    private String nombreReceptor;
    private String domicilioFiscalReceptor;
    private String regimenFiscalReceptor;
    private String nombreAlumno;
    private String curp;
    private String nivelEducativo;
    private String autRVOE;
    private java.math.BigDecimal importe;
    private boolean conIVA;
    // getters y setters...
}

// application/dto/CfdiResponseDto.java
public class CfdiResponseDto {
    private String xmlFirmado;

    public CfdiResponseDto(String xmlFirmado) {
        this.xmlFirmado = xmlFirmado;
    }
    // getter...
}
```

---

## Paso 7: Configuración de Spring

### `CfdiProperties.java` — Reemplaza las rutas hardcodeadas

```java
// infrastructure/config/CfdiProperties.java
package mx.cdefis.cfdi.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cfdi")
public class CfdiProperties {

    private String keyPath;       // ruta al .key
    private String cerName;       // nombre del .cer en classpath
    private String keyPassword;   // contraseña del .key

    // getters y setters...
}
```

### `application.properties`

```properties
# Configuración del CSD (Certificado de Sello Digital)
cfdi.cer-name=prueba.cer
cfdi.key-path=src/main/resources/csd/prueba.key
cfdi.key-password=12345678a
```

> En producción usar variables de entorno o Spring Cloud Config para no exponer la contraseña:
> ```properties
> cfdi.key-password=${CFDI_KEY_PASSWORD}
> ```

---

## Paso 8: Migrar los recursos estáticos

Copiar sin cambios al mismo directorio `src/main/resources/`:

```
src/main/resources/
├── csd/
│   ├── prueba.cer
│   └── prueba.key
├── xsd/
│   ├── cfdv40.xsd
│   ├── iedu.xsd
│   ├── tdCFDI.xsd
│   └── catCFDI.xsd
└── xslt/
    ├── cadenaoriginal_4_0.xslt
    └── complements/
        └── *.xslt
```

**Diferencia clave:** En el proyecto original los archivos se leen con rutas relativas del
filesystem (`"src/main/resources/xslt/..."`). En Spring Boot siempre se usa el classpath:

```java
// ANTES (proyecto original)
new StreamSource("src/main/resources/xslt/cadenaoriginal_4_0.xslt")

// DESPUÉS (Spring Boot)
new StreamSource(
    getClass().getClassLoader().getResourceAsStream("xslt/cadenaoriginal_4_0.xslt")
)
```

---

## Paso 9: Ajustar el pom.xml

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
</parent>

<groupId>mx.cdefis</groupId>
<artifactId>cfdi-service</artifactId>
<version>1.0.0</version>

<properties>
    <java.version>21</java.version>
</properties>

<dependencies>
    <!-- Spring Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JAXB (ya no viene incluido en Spring Boot 3, hay que declararlo) -->
    <dependency>
        <groupId>jakarta.xml.bind</groupId>
        <artifactId>jakarta.xml.bind-api</artifactId>
        <version>4.0.0</version>
    </dependency>
    <dependency>
        <groupId>org.glassfish.jaxb</groupId>
        <artifactId>jaxb-runtime</artifactId>
        <version>4.0.3</version>
    </dependency>

    <!-- BouncyCastle -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
        <version>1.78.1</version>
    </dependency>
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcpkix-jdk18on</artifactId>
        <version>1.78.1</version>
    </dependency>

    <!-- Tests -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Plugin de Spring Boot -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>

        <!-- JAXB: generación de Comprobante.java desde el XSD (igual que antes) -->
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>jaxb2-maven-plugin</artifactId>
            <version>3.1.0</version>
            <executions>
                <execution>
                    <goals><goal>xjc</goal></goals>
                </execution>
            </executions>
            <configuration>
                <sources>
                    <source>src/main/resources/xsd/cfdv40.xsd</source>
                </sources>
                <packageName>mx.cdefis.cfdi.model</packageName>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## Problemas conocidos y cómo resolverlos

### 1. Rutas hardcodeadas del .key

**Problema:** `CfdiSigner` lee el `.key` con `Files.readAllBytes(Paths.get(keyPath))` usando
una ruta relativa del filesystem.

**Solución en Spring Boot:** Inyectar la ruta desde `CfdiProperties` y en producción usar una
ruta absoluta configurable, o cargar el `.key` desde el classpath con
`getClass().getClassLoader().getResourceAsStream("csd/prueba.key")`.

---

### 2. `CfdiCadenaAdapter` escribe/lee archivos temporales

**Problema:** El flujo original escribe `cfdi.xml` en disco para luego leerlo.

**Solución:** Usar `StringWriter`/`StringReader` en memoria (ya resuelto en el Paso 5 arriba).

---

### 3. `NamespacePrefixMapper` es específico de GlassFish JAXB

**Problema:** `org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper` es una API
privada de la implementación de referencia.

**Solución:** La dependencia `jaxb-runtime` de GlassFish ya viene declarada, así que
`NamespacePrefixMapperImpl` funciona igual. Solo asegurarse de que la versión de
`jaxb-runtime` sea compatible con Spring Boot 3.

---

### 4. `JAXBContext` es costoso de crear

**Problema:** `JAXBContext.newInstance(...)` es una operación lenta. Si se crea en cada
request hay degradación de rendimiento.

**Solución:** Crear el `JAXBContext` una sola vez como bean de Spring (en `@Bean` o en el
constructor del `@Component`) y reutilizarlo. El `JAXBContext` es thread-safe; el
`Marshaller` no lo es (crear uno por request está bien).

---

### 5. Manejo de excepciones en el controlador

Agregar un `@ControllerAdvice` para manejar errores de generación:

```java
@RestControllerAdvice
public class CfdiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleCfdiError(Exception e) {
        return ResponseEntity.internalServerError()
                .body("Error generando CFDI: " + e.getMessage());
    }
}
```

---

## Resumen del orden de implementación

```
1. pom.xml          ← Agregar Spring Boot parent + spring-boot-starter-web
2. Puertos (in/out) ← Definir interfaces (solo código, sin lógica)
3. Modelos dominio  ← CfdiIeduRequest, CfdiIngresoRequest (POJOs simples)
4. Adaptadores out  ← CfdiSignerAdapter, CfdiXmlAdapter, CfdiCadenaAdapter, CfdiValidatorAdapter
5. Servicios        ← CfdiIeduService, CfdiIngresoService (implementan casos de uso)
6. DTOs             ← CfdiIeduRequestDto, CfdiIngresoRequestDto, CfdiResponseDto
7. Controlador      ← CfdiController (@RestController)
8. Configuración    ← CfdiProperties + application.properties
9. Main class       ← @SpringBootApplication
```
