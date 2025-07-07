/*
 * *
 *  * The contents of this file are subject to the license and copyright
 *  * detailed in the LICENSE and NOTICE files at the root of the source
 *  * tree and available online at
 *  *
 *  * http://www.dspace.org/license/
 *
 */

package de.leuphana.escience.pure.apiobjects;

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
