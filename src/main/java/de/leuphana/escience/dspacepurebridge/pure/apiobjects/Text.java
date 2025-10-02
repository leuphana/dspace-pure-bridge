/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

import org.apache.commons.lang3.StringUtils;

public class Text {
    private String en_GB;
    private String de_DE;

    public String getEn_GB() {
        return en_GB;
    }

    public void setEn_GB(String en_GB) {
        this.en_GB = en_GB;
    }

    public String getDe_DE() {
        return de_DE;
    }

    public void setDe_DE(String de_DE) {
        this.de_DE = de_DE;
    }

    @Override
    public String toString() {
        return "Text{" +
            "en_GB='" + en_GB + '\'' +
            ", de_DE='" + de_DE + '\'' +
            '}';
    }

    public String getText() {
        if (StringUtils.isNotEmpty(de_DE)) {
            return de_DE;
        }
        return en_GB;
    }
}
