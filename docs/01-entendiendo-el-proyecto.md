# Entendiendo el proyecto: generar_cfdi

Generador de CFDI 4.0 en Java puro (sin framework). Produce comprobantes fiscales digitales
firmados y válidos conforme al SAT de México.

---

## Tabla de contenido

1. [¿Qué hace este proyecto?](#1-qué-hace-este-proyecto)
2. [Flujo completo de generación](#2-flujo-completo-de-generación)
3. [Estructura de archivos](#3-estructura-de-archivos)
4. [Descripción de cada clase](#4-descripción-de-cada-clase)
5. [Recursos estáticos](#5-recursos-estáticos)
6. [Dependencias Maven](#6-dependencias-maven)
7. [Cómo ejecutar](#7-cómo-ejecutar)
8. [Datos de prueba incluidos](#8-datos-de-prueba-incluidos)
9. [Conceptos fiscales clave](#9-conceptos-fiscales-clave)

---

## 1. ¿Qué hace este proyecto?

Genera **CFDI 4.0** (Comprobante Fiscal Digital por Internet) válidos, listos para timbrarse
con un PAC (Proveedor Autorizado de Certificación). Cubre dos tipos de comprobante:

| Tipo | Clase | Caso de uso |
|------|-------|-------------|
| Ingreso con complemento IEDU | `GeneradorIEDU` | Colegiaturas, inscripciones, educación |
| Ingreso general | `GeneradorIngresoGeneral` | Venta de uniformes, artículos escolares, etc. |

Ambos tipos soportan emitirse **con IVA (16%)** o **sin IVA**.

---

## 2. Flujo completo de generación

```
[ Generador ] ──► Comprobante (objeto Java)
                         │
                         ▼
              [ CfdiProcessor ]
                         │
          ┌──────────────┼──────────────────┐
          │              │                  │
          ▼              ▼                  ▼
   Leer certificado  Marshal JAXB      Configurar
   (.cer / .key)    → cfdi.xml         namespaces
          │              │
          │              ▼
          │    [ CfdiCadenaOriginalGenerator ]
          │         Aplica XSLT
          │    → cadena original (String)
          │              │
          ▼              ▼
   [ CfdiSigner ]
   SHA256withRSA
   → sello (Base64)
          │
          ▼
   Comprobante.setSello(sello)
          │
          ▼
   Marshal JAXB → cfdi_firmado.xml
          │
          ▼
   [ CfdiValidator ]
   Valida contra XSD
```

---

## 3. Estructura de archivos

```
generar_cfdi/
├── src/main/java/mx/cdefis/cfdi/
│   ├── Main.java                                  # Punto de entrada
│   ├── CfdiProcessor.java                         # Orquestador del pipeline
│   ├── GeneradorIEDU.java                         # Genera CFDI con complemento educativo
│   ├── GeneradorIngresoGeneral.java               # Genera CFDI de ingreso general
│   ├── cadena/
│   │   └── CfdiCadenaOriginalGenerator.java       # Aplica XSLT → cadena original
│   ├── model/
│   │   └── InstEducativas.java                    # Modelo JAXB del complemento IEDU
│   ├── sign/
│   │   └── CfdiSigner.java                        # Firma digital RSA + manejo de .cer
│   ├── util/
│   │   └── NamespacePrefixMapperImpl.java         # Prefijos XML (cfdi:, iedu:, xsi:)
│   └── validator/
│       └── CfdiValidator.java                     # Validación contra XSD
│
├── src/main/resources/
│   ├── csd/
│   │   ├── prueba.cer                             # Certificado de prueba SAT
│   │   └── prueba.key                             # Llave privada cifrada (PKCS#8)
│   ├── xsd/
│   │   ├── cfdv40.xsd                             # Esquema CFDI 4.0
│   │   ├── iedu.xsd                               # Esquema complemento IEDU
│   │   ├── tdCFDI.xsd                             # Tipos de datos CFDI
│   │   └── catCFDI.xsd                            # Catálogos CFDI
│   └── xslt/
│       ├── cadenaoriginal_4_0.xslt                # Transforma XML → cadena para firma
│       └── complements/                           # XSLT de ~30 complementos SAT
│           ├── iedu.xslt
│           ├── nomina12.xslt
│           ├── Pagos10.xslt / pagos20.xslt
│           └── ... (CartaPorte, ComercioExterior, etc.)
│
└── target/generated-sources/jaxb/mx/cdefis/cfdi/model/
    ├── Comprobante.java                           # Generado desde cfdv40.xsd
    ├── ObjectFactory.java
    └── ...
```

> **Nota:** Las clases en `target/generated-sources/` son generadas automáticamente por el
> plugin `jaxb2-maven-plugin` a partir de `cfdv40.xsd`. No se editan a mano.

---

## 4. Descripción de cada clase

### `Main.java`
Punto de entrada. Descomenta la línea que corresponde al tipo de CFDI a generar y llama a
`CfdiProcessor.process()`.

```java
// Opciones disponibles:
Comprobante comprobante = GeneradorIEDU.generar(false);          // IEDU sin IVA
Comprobante comprobante = GeneradorIEDU.generar(true);           // IEDU con IVA
Comprobante comprobante = GeneradorIngresoGeneral.generar(true); // Ingreso con IVA
Comprobante comprobante = GeneradorIngresoGeneral.generar(false);// Ingreso sin IVA
```

---

### `GeneradorIEDU.java`
Construye un `Comprobante` para servicios educativos. Agrega el complemento `InstEducativas`
a nivel de concepto (`ComplementoConcepto`).

Campos del complemento IEDU que se configuran aquí:

| Campo | Ejemplo |
|-------|---------|
| `nombreAlumno` | `"JUAN PEREZ LOPEZ"` |
| `CURP` | `"PELO800101HDFRRL09"` |
| `nivelEducativo` | `"Bachillerato o su equivalente"` |
| `autRVOE` | `"123456"` |

- Clave de producto SAT: `86121500` (Servicios de educación)
- Clave de unidad: `E48` (Servicio)
- `ObjetoImp`: `"02"` con IVA / `"01"` sin IVA

---

### `GeneradorIngresoGeneral.java`
Construye un `Comprobante` de ingreso genérico. Sin complemento adicional.

- Receptor: `XAXX010101000` / `"PUBLICO GENERAL"`
- Clave de producto SAT: `53102700` (Uniformes)
- Clave de unidad: `H87` (Pieza)
- `UsoCFDI`: `S01` (Sin efectos fiscales)

---

### `CfdiProcessor.java`
Orquesta todo el pipeline dado un `Comprobante` ya construido:

1. Crea el `JAXBContext` con `Comprobante` e `InstEducativas`
2. Configura el `Marshaller` con `NamespacePrefixMapperImpl` y `schemaLocation`
3. Lee certificado → `getCertificadoBase64()` y `getNoCertificado()`
4. Serializa a `cfdi.xml` (sin sello)
5. Llama a `CfdiCadenaOriginalGenerator.generarCadena()` → cadena original
6. Firma con `CfdiSigner.generarSello()` → sello Base64
7. Serializa a `cfdi_firmado.xml` (con sello)
8. Valida con `CfdiValidator.validate()`

> **Problema actual:** rutas de archivos son hardcodeadas (`"src/main/resources/csd/prueba.key"`).
> En Spring Boot esto debe venir de configuración o `classpath:`.

---

### `CfdiCadenaOriginalGenerator.java`
Aplica la transformación XSLT definida por el SAT para extraer los campos del XML en el
orden y formato exacto requerido para la firma.

```
XML sin sello  ──[XSLT cadenaoriginal_4_0.xslt]──►  ||version|fecha|...|
```

La cadena resultante es el input que se firma con la llave privada.

---

### `CfdiSigner.java`
Maneja toda la criptografía usando **BouncyCastle**.

| Método | Qué hace |
|--------|----------|
| `getPrivateKey(keyPath, password)` | Descifra la llave PKCS#8 con la contraseña y retorna `PrivateKey` |
| `generarSello(cadena, keyPath, password)` | Firma con `SHA256withRSA`, retorna Base64 |
| `getCertificadoBase64(cerName)` | Lee el `.cer` desde classpath, retorna Base64 |
| `getNoCertificado(cerName)` | Lee el serial del certificado X.509, retorna últimos 20 dígitos |

El archivo `.key` del SAT viene cifrado con el algoritmo `PKCS#8 + AES` (no es PEM estándar).
BouncyCastle es necesario porque el JDK estándar no soporta este cifrado directamente.

---

### `CfdiValidator.java`
Valida el XML firmado contra uno o más archivos XSD usando `javax.xml.validation`.
Acepta múltiples XSDs para validar el CFDI base + sus complementos en una sola pasada.

---

### `InstEducativas.java`
Modelo JAXB manual del complemento IEDU. Mapeado al namespace `http://www.sat.gob.mx/iedu`.
Se inyecta en el `ComplementoConcepto` del concepto educativo.

---

### `NamespacePrefixMapperImpl.java`
Extiende `NamespacePrefixMapper` de GlassFish JAXB para que el XML generado use los prefijos
estándar del SAT en lugar de los autogenerados (`ns2`, `ns3`, etc.):

| Namespace | Prefijo |
|-----------|---------|
| `http://www.sat.gob.mx/cfd/4` | `cfdi` |
| `http://www.sat.gob.mx/iedu` | `iedu` |
| `http://www.w3.org/2001/XMLSchema-instance` | `xsi` |

---

## 5. Recursos estáticos

### `csd/` — Certificados de Sello Digital

| Archivo | Descripción |
|---------|-------------|
| `prueba.cer` | Certificado X.509 de prueba emitido por el SAT |
| `prueba.key` | Llave privada cifrada (PKCS#8 + AES). Contraseña: `12345678a` |

> En producción estos archivos se reemplazan con el CSD real de la empresa emisora.
> **Nunca commitear llaves privadas reales al repositorio.**

### `xsd/` — Esquemas de validación

| Archivo | Valida |
|---------|--------|
| `cfdv40.xsd` | Estructura completa del CFDI 4.0 |
| `iedu.xsd` | Complemento de instituciones educativas |
| `tdCFDI.xsd` | Tipos de datos compartidos |
| `catCFDI.xsd` | Catálogos (monedas, países, usos CFDI, etc.) |

### `xslt/` — Transformaciones

| Archivo | Uso |
|---------|-----|
| `cadenaoriginal_4_0.xslt` | Genera la cadena original del CFDI 4.0 (requerida para firma) |
| `complements/*.xslt` | Cadenas originales de cada complemento SAT (~30 archivos) |

---

## 6. Dependencias Maven

```xml
<!-- JAXB: serialización Java <-> XML -->
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

<!-- BouncyCastle: criptografía (descifrado de .key SAT) -->
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
```

**Plugin de build:**
- `jaxb2-maven-plugin`: genera `Comprobante.java` y demás modelos desde `cfdv40.xsd`
  en `target/generated-sources/jaxb/mx/cdefis/cfdi/model/`

---

## 7. Cómo ejecutar

**Requisitos:** Java 21, Maven 3.8+

```bash
# 1. Compilar y generar modelos JAXB
mvn compile

# 2. Ejecutar
mvn exec:java -Dexec.mainClass="mx.cdefis.cfdi.Main"

# O desde el IDE: Run > Main.java
```

Archivos generados en la raíz del proyecto:
- `cfdi.xml` — CFDI sin sello
- `cfdi_firmado.xml` — CFDI firmado y validado

---

## 8. Datos de prueba incluidos

| Campo | Valor |
|-------|-------|
| RFC Emisor | `EKU9003173C9` |
| Nombre Emisor | `ESCUELA KEMPER URGATE` |
| Régimen Fiscal | `601` (General de Ley) |
| RFC Receptor (IEDU) | `CUSC850516316` |
| RFC Receptor (General) | `XAXX010101000` (Público General) |
| Contraseña del .key | `12345678a` |
| Lugar de expedición | `58260` (CP Michoacán) |

> Estos datos son del ambiente de pruebas del SAT. No generan obligaciones fiscales reales.

---

## 9. Conceptos fiscales clave

| Término | Descripción |
|---------|-------------|
| **CFDI** | Comprobante Fiscal Digital por Internet. Factura electrónica mexicana |
| **SAT** | Servicio de Administración Tributaria (autoridad fiscal de México) |
| **CSD** | Certificado de Sello Digital. Par `.cer` + `.key` que identifica al emisor |
| **Cadena original** | String canónico extraído del XML mediante XSLT, que se firma |
| **Sello** | Firma digital SHA256withRSA del emisor sobre la cadena original, en Base64 |
| **Complemento** | Sección adicional del CFDI con información específica (IEDU, Nómina, Pagos, etc.) |
| **PAC** | Proveedor Autorizado de Certificación. Timbra el CFDI ante el SAT |
| **RVOE** | Reconocimiento de Validez Oficial de Estudios (para complemento IEDU) |
| **CURP** | Clave Única de Registro de Población (ID único de personas en México) |
| **ObjetoImp** | Campo que indica si el concepto es objeto de impuesto: `01`=No, `02`=Sí |
