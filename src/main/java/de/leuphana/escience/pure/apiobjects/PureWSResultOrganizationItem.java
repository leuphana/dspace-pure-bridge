/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.pure.apiobjects;

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
