package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

public class Value {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Value{" +
            "value='" + value + '\'' +
            '}';
    }
}
