package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

import java.util.List;

public class Texts {
    private List<Text> text;
    public List<Text> getText() {
        return text;
    }
    public void setText(List<Text> text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "Texts{" +
            "text=" + text +
            '}';
    }
}
