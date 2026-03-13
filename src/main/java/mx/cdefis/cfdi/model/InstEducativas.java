package mx.cdefis.cfdi.model;

import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "instEducativas", namespace = "http://www.sat.gob.mx/iedu")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
public class InstEducativas {

    @XmlAttribute(name = "version", required = true)
    private String version = "1.0";

    @XmlAttribute(name = "nombreAlumno", required = true)
    private String nombreAlumno;

    @XmlAttribute(name = "CURP", required = true)
    private String curp;

    @XmlAttribute(name = "nivelEducativo", required = true)
    private String nivelEducativo;

    @XmlAttribute(name = "autRVOE", required = true)
    private String autRVOE;

    // getters y setters

    public String getAutRVOE() {
        return autRVOE;
    }

    public void setAutRVOE(String autRVOE) {
        this.autRVOE = autRVOE;
    }

    public String getCurp() {
        return curp;
    }

    public void setCurp(String curp) {
        this.curp = curp;
    }

    public String getNivelEducativo() {
        return nivelEducativo;
    }

    public void setNivelEducativo(String nivelEducativo) {
        this.nivelEducativo = nivelEducativo;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getNombreAlumno() {
        return nombreAlumno;
    }

    public void setNombreAlumno(String nombreAlumno) {
        this.nombreAlumno = nombreAlumno;
    }
}