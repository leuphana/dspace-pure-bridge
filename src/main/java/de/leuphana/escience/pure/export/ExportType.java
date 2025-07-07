/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import de.leuphana.escience.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.pure.apiobjects.PureWSResultStudentThesisItem;
import de.leuphana.escience.pure.apiobjects.PureWSResults;

public enum ExportType {
    RESEARCH_OUTPUT("researchOutput", "research-outputs", null), STUDENT_THESIS("studentThesis", "student-theses",
        PureWSStudentThesisResults.class);

    private final String mappingSuffix;
    private final String pureEndpoint;
    private final Class<? extends PureWSResults<? extends PureWSResultItem>> searchResultClass;

    ExportType(String mappingSuffix, String pureEndpoint,
               Class<? extends PureWSResults<? extends PureWSResultItem>> searchResultClass) {
        this.mappingSuffix = mappingSuffix;
        this.pureEndpoint = pureEndpoint;
        this.searchResultClass = searchResultClass;
    }

    public String getMappingSuffix() {
        return mappingSuffix;
    }

    public String getPureEndpoint() {
        return pureEndpoint;
    }

    public Class<? extends PureWSResults<? extends PureWSResultItem>> getSearchResultClass() {
        return searchResultClass;
    }

    static class PureWSStudentThesisResults extends PureWSResults<PureWSResultStudentThesisItem> {
    }

}
