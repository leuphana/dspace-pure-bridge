/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.dspacepurebridge.pure.export;

import java.util.ArrayList;
import java.util.List;

import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.ResearchOutputApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;

public enum ResearchOutputMappingType  {
    TYPE(api -> api.researchOutputGetAllowedTypes().getClassifications()),
    CATEGORY(api -> api.researchoutputGetAllowedCategories().getClassifications()),
    PUBLISHED(api -> api.researchoutputGetAllowedPublicationStatuses().getClassifications()),
    LANGUAGE(api -> api.researchoutputGetAllowedLanguages().getClassifications()),
    ACCESS_TYPE(api -> api.researchoutputGetAllowedElectronicVersionAccessTypes().getClassifications()),
    ROLE(api -> api.researchoutputGetAllowedBookAnthologyContributorRoles().getClassifications()),
    ELECTRONIC_VERSION_TYPE(api -> api.researchoutputGetAllowedElectronicVersionVersionTypes().getClassifications()),
    LICENSE(api -> api.researchoutputGetAllowedElectronicVersionLicenseTypes().getClassifications());

    private final PureResearchOutputClassificationRefFetcher pureResearchOutputClassificationRefFetcher;
    private final List<ClassificationRef> classificationRefs = new ArrayList<>();

    ResearchOutputMappingType(PureResearchOutputClassificationRefFetcher pureResearchOutputClassificationRefFetcher) {
        this.pureResearchOutputClassificationRefFetcher = pureResearchOutputClassificationRefFetcher;
    }

    List<ClassificationRef> getClassificationRefs(ResearchOutputApi researchOutputApi) throws ApiException {
        if (classificationRefs.isEmpty()) {
            classificationRefs.addAll(pureResearchOutputClassificationRefFetcher.fetch(researchOutputApi));
        }
        return classificationRefs;
    }
}
