/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.dspacepurebridge.pure.export;

public enum DSpaceLanguage {
    GERMAN("de_DE", "de", "deu"),
    ENGLISH("en_GB", "en", "eng");

    private final String locale;
    private final String iso2Letter;
    private final String iso3Letter;
    DSpaceLanguage(String locale, String iso2Letter, String iso3Letter) {
        this.locale = locale;
        this.iso2Letter = iso2Letter;
        this.iso3Letter = iso3Letter;
    }

    public String getLocale() {
        return locale;
    }

    public String getIso2Letter() {
        return iso2Letter;
    }

    public String getIso3Letter() {
        return iso3Letter;
    }
}
