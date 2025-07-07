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

public class Link {
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Link{" +
            "url='" + url + '\'' +
            '}';
    }
}
