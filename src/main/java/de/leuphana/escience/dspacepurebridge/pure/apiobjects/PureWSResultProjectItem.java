/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

public class PureWSResultProjectItem extends PureWSResultItem {
    private Text title;

    public Text getTitle() {
        return title;
    }

    public void setTitle(Text title) {
        this.title = title;
    }
}
