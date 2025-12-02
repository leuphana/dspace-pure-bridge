package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

public class PureWSResultPersonItem extends PureWSResultItem {
    private PersonName name;

    private String orcid;

    public PersonName getName() {
        return name;
    }

    public void setName(PersonName name) {
        this.name = name;
    }

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }
}
