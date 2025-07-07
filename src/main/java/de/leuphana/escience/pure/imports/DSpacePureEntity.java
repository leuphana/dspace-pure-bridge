/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.pure.imports;

import de.leuphana.escience.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.pure.apiobjects.PureWSResultOrganizationItem;
import de.leuphana.escience.pure.apiobjects.PureWSResultPersonItem;
import de.leuphana.escience.pure.apiobjects.PureWSResultProjectItem;
import de.leuphana.escience.pure.apiobjects.PureWSResults;


public enum DSpacePureEntity {

    PERSON("persons", PureWSPersonResults.class, "PurePerson","Person",
           "300000000/9", "300000000/3", "isPurePersonOfPerson"),
    ORGANIZATION("organizations", PureWSOrganizationResults.class,
        "PureOrgUnit", "OrgUnit",
                 "300000000/11", "300000000/4", "isPureOrgUnitOfOrgUnit"),
    PROJECT("projects", PureWSProjectResults.class, "PureProject", "Project",
            "300000000/10", "300000000/5", "isPureProjectOfProject" );

    private final String endpoint;
    private final Class<? extends PureWSResults<? extends PureWSResultItem>> resultsClass;
    private final String dspacePureEntity;
    private final String dspaceEntity;
    private final String dspacePureEntityCollectionHandle;
    private final String dspaceEntityCollectionHandle;
    private final String dspaceEntityRelationshipType;

    DSpacePureEntity(String endpoint, Class<? extends PureWSResults<? extends PureWSResultItem>> resultsClass,
                     String dspacePureEntity, String dspaceEntity,
                     String dspacePureEntityCollectionHandle, String dspaceEntityCollectionHandle,
                     String dspaceEntityRelationshipType) {
        this.endpoint = endpoint;
        this.resultsClass = resultsClass;
        this.dspacePureEntity = dspacePureEntity;
        this.dspaceEntity = dspaceEntity;
        this.dspacePureEntityCollectionHandle = dspacePureEntityCollectionHandle;
        this.dspaceEntityCollectionHandle = dspaceEntityCollectionHandle;
        this.dspaceEntityRelationshipType = dspaceEntityRelationshipType;
    }

    public String getDspacePureEntityCollectionHandle() {
        return dspacePureEntityCollectionHandle;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getDspacePureEntity() {
        return dspacePureEntity;
    }

    public Class<? extends PureWSResults<? extends PureWSResultItem>> getResultsClass() {
        return resultsClass;
    }

    public String getDspaceEntity() {
        return dspaceEntity;
    }

    public String getDspaceEntityCollectionHandle() {
        return dspaceEntityCollectionHandle;
    }

    public String getDspaceEntityRelationshipType() {
        return dspaceEntityRelationshipType;
    }

    static class PureWSOrganizationResults extends PureWSResults<PureWSResultOrganizationItem> {
    }

    static class PureWSProjectResults extends PureWSResults<PureWSResultProjectItem> {
    }

    static class PureWSPersonResults extends PureWSResults<PureWSResultPersonItem> {
    }

}
