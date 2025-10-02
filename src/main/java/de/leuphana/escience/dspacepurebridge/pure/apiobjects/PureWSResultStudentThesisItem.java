/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

import java.util.List;

public class PureWSResultStudentThesisItem extends PureWSResultItem {

    private Value title;
    private List<Link> links;

    public Value getTitle() {
        return title;
    }

    public void setTitle(Value title) {
        this.title = title;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @Override
    public String toString() {
        return "PureWSResultStudentThesisItem{" + "title=" + title + ", links=" + links + '}';
    }
}
