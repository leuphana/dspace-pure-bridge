package de.leuphana.escience.dspacepurebridge.search;

import de.leuphana.escience.dspacepurebridge.Constants;

public enum SearchQueryType {
    PERSON_CACHE_IMPORT("search.entitytype:Person", null),
    PROJECT_CACHE_IMPORT("search.entitytype:Project", null),
    ORGANIZATION_CACHE_IMPORT("search.entitytype:OrgUnit", null),
    PUBLICATION_EXPORT("search.entitytype:Publication", "!" + Constants.SCHEME + "." + Constants.ELEMENT + "." + Constants.UUID_QUALIFIER + ":*"),
    ORGANIZATION_CACHE_EXPORT("search.entitytype:OrgUnit", "organization.legalName:* AND " + Constants.SCHEME + "." + Constants.ELEMENT + "." + Constants.UUID_QUALIFIER + ":*");

    private final String query;
    private final String filter;

    SearchQueryType(String query, String filter) {
        this.query = query;
        this.filter = filter;
    }

    public String getQuery() {
        return query;
    }

    public String getFilter() {
        return filter;
    }
}
