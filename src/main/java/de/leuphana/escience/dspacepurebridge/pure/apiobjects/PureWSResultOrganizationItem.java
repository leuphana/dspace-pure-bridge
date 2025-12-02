package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

public class PureWSResultOrganizationItem extends PureWSResultItem {
    private Text name;

    public Text getName() {
        return name;
    }

    public void setName(Text name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PureWSResultOrganizationItem{" +
            "name=" + name +
            '}';
    }
}
