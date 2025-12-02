package de.leuphana.escience.dspacepurebridge.pure.imports;

import de.leuphana.escience.dspacepurebridge.pure.apiobjects.*;
import org.dspace.services.ConfigurationService;


public enum DSpacePureEntity {

    PERSON("persons", PureWSPersonResults.class, "PurePerson",
            "dspace-pure-bridge.entities.purePerson.collection", "isPurePersonOfPerson"),
    ORGANIZATION("organizations", PureWSOrganizationResults.class,
        "PureOrgUnit",
            "dspace-pure-bridge.entities.pureOrgUnit.collection", "isPureOrgUnitOfOrgUnit"),
    PROJECT("projects", PureWSProjectResults.class, "PureProject",
            "dspace-pure-bridge.entities.pureProject.collection", "isPureProjectOfProject" );

    private final String endpoint;
    private final Class<? extends PureWSResults<? extends PureWSResultItem>> resultsClass;
    private final String dspacePureEntity;
    private final String dspacePureEntityCollectionHandleKey;
    private final String dspaceEntityRelationshipType;

    DSpacePureEntity(String endpoint, Class<? extends PureWSResults<? extends PureWSResultItem>> resultsClass,
                     String dspacePureEntity,
                     String dspacePureEntityCollectionHandleKey,
                     String dspaceEntityRelationshipType) {
        this.endpoint = endpoint;
        this.resultsClass = resultsClass;
        this.dspacePureEntity = dspacePureEntity;
        this.dspacePureEntityCollectionHandleKey = dspacePureEntityCollectionHandleKey;
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
