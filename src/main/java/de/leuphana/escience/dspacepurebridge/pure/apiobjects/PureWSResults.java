package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

import java.util.List;


public abstract class PureWSResults<T extends PureWSResultItem> {

    private int count;

    private List<T> items;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
