/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

import java.util.List;

public class OrganizationName {
    private List<Text> text;

    public List<Text> getText() {
        return text;
    }

    public void setText(List<Text> text) {
        this.text = text;
    }
}
