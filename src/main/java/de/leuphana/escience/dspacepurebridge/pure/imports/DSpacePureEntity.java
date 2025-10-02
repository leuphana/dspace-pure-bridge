/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge.pure.imports;

import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultOrganizationItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultPersonItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultProjectItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResults;
import org.dspace.services.ConfigurationService;


public enum DSpacePureEntity {

    PERSON("persons", PureWSPersonResults.class, "PurePerson","Person",
           "dspace-pure-bridge.entities.purePerson.collection", "dspace-pure-bridge.entities.dspacePerson.collection", "isPurePersonOfPerson"),
    ORGANIZATION("organizations", PureWSOrganizationResults.class,
        "PureOrgUnit", "OrgUnit",
                 "dspace-pure-bridge.entities.pureOrgUnit.collection", "dspace-pure-bridge.entities.dspaceOrgUnit.collection", "isPureOrgUnitOfOrgUnit"),
    PROJECT("projects", PureWSProjectResults.class, "PureProject", "Project",
            "dspace-pure-bridge.entities.pureProject.collection", "dspace-pure-bridge.entities.dspaceProject.collection", "isPureProjectOfProject" );

    private final String endpoint;
    private final Class<? extends PureWSResults<? extends PureWSResultItem>> resultsClass;
    private final String dspacePureEntity;
    private final String dspaceEntity;
    private final String dspacePureEntityCollectionHandleKey;
    private final String dspaceEntityCollectionHandleKey;
    private final String dspaceEntityRelationshipType;

    DSpacePureEntity(String endpoint, Class<? extends PureWSResults<? extends PureWSResultItem>> resultsClass,
                     String dspacePureEntity, String dspaceEntity,
                     String dspacePureEntityCollectionHandleKey, String dspaceEntityCollectionHandleKey,
                     String dspaceEntityRelationshipType) {
        this.endpoint = endpoint;
        this.resultsClass = resultsClass;
        this.dspacePureEntity = dspacePureEntity;
        this.dspaceEntity = dspaceEntity;
        this.dspacePureEntityCollectionHandleKey = dspacePureEntityCollectionHandleKey;
        this.dspaceEntityCollectionHandleKey = dspaceEntityCollectionHandleKey;
        this.dspaceEntityRelationshipType = dspaceEntityRelationshipType;
    }

    public String getDspacePureEntityCollectionHandle(ConfigurationService configurationService) {
        return configurationService.getProperty(dspacePureEntityCollectionHandleKey);
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

    public String getDspaceEntityCollectionHandle(ConfigurationService configurationService) {
        return configurationService.getProperty(dspaceEntityCollectionHandleKey);
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
