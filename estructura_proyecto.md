# Estructura del Proyecto Generarcfdi

```
.
├── .env
├── .gitignore
├── .idea/
├── .mvn/
├── README.md
├── cfdi-core.iml
├── cfdi.xml
├── cfdi_firmado.xml
├── cfdi_firmadoPrueba.xml
├── cfdi_iedu.xml
├── cfdi_ieduP.xml
├── cfdi_ieduPrueba.xml
├── docs/
│   ├── 01-entendiendo-el-proyecto.md
│   └── 02-migracion-a-springboot.md
├── logs/
│   ├── errors/
│   ├── xml/
│   │   ├── cfdi.xml
│   │   └── cfdi_firmado.xml
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── mx/
│   │   │       └── cdefis/
│   │   │           └── cfdi/
│   │   │               ├── CfdiProcessor.java
│   │   │               ├── Main.java
│   │   │               ├── builder/
│   │   │               │   ├── CfdiBuilder.java
│   │   │               │   ├── GeneradorIEDU.java
│   │   │               │   └── GeneradorIngresoGeneral.java
│   │   │               ├── cadena/
│   │   │               │   └── CfdiCadenaOriginalGenerator.java
│   │   │               ├── model/
│   │   │               │   └── InstEducativas.java
│   │   │               ├── pac/
│   │   │               ├── sello/
│   │   │               ├── sign/
│   │   │               │   └── CfdiSigner.java
│   │   │               ├── timbrado/
│   │   │               │   └── soap/
│   │   │               │       └── TimbradoException.java
│   │   │               ├── util/
│   │   │               │   ├── NamespacePrefixMapperImpl.java
│   │   │               │   └── XmlUtils.java
│   │   │               ├── validator/
│   │   │               │   └── CfdiValidator.java
│   │   │               └── xml/
│   │   └── resources/
│   │       ├── csd/
│   │       │   ├── prueba.cer
│   │       │   └── prueba.key
│   │       ├── xsd/
│   │       │   ├── TimbreFiscalDigitalv11.xsd
│   │       │   ├── catCFDI.xsd
│   │       │   ├── cfdv40.xsd
│   │       │   ├── iedu.xsd
│   │       │   └── tdCFDI.xsd
│   │       └── xslt/
│   │           ├── cadenaoriginal_4_0.xslt
│   │           └── complements/
│   │               ├── CartaPorte30.xslt
│   │               ├── CartaPorte31.xslt
│   │               ├── ComercioExterior11.xslt
│   │               ├── ComercioExterior20.xslt
│   │               ├── GastosHidrocarburos10.xslt
│   │               ├── IngresosHidrocarburos.xslt
│   │               ├── Pagos10.xslt
│   │               ├── TuristaPasajeroExtranjero.xslt
│   │               ├── aerolineas.xslt
│   │               ├── certificadodedestruccion.xslt
│   │               ├── cfdiregistrofiscal.xslt
│   │               ├── consumodeCombustibles11.xslt
│   │               ├── detallista.xslt
│   │               ├── divisas.xslt
│   │               ├── donat11.xslt
│   │               ├── ecc12.xslt
│   │               ├── iedu.xslt
│   │               ├── implocal.xslt
│   │               ├── ine11.xslt
│   │               ├── leyendasFisc.xslt
│   │               ├── nomina12.xslt
│   │               ├── notariospublicos.xslt
│   │               ├── obrasarteantiguedades.xslt
│   │               ├── pagoenespecie.xslt
│   │               ├── pagos20.xslt
│   │               ├── pfic.xslt
│   │               ├── renovacionysustitucionvehiculos.xslt
│   │               ├── servicioparcialconstruccion.xslt
│   │               ├── utilerias.xslt
│   │               ├── valesdedespensa.xslt
│   │               ├── vehiculousado.xslt
│   │               └── ventavehiculos11.xslt
│   └── test/
│       └── java/
├── storage/
│   └── cfdi/
│       └── timbrados/
│           └── 2026/
│               └── 03/
│                   ├── 413BD93A-BB48-5C1D-B00C-825AB76B4388.xml
│                   └── 507CDF55-8994-594B-B5B5-668252EFFF51.xml
├── target/
│   ├── classes/
│   ├── generated-sources/
│   ├── jaxb2/
│   ├── jaxws/
│   ├── maven-status/
│   └── test-classes/
```
