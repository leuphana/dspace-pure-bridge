/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.leuphana.escience.pure.generated.ApiException;
import de.leuphana.escience.pure.generated.api.ResearchOutputApi;
import de.leuphana.escience.pure.generated.model.BookAnthology;
import de.leuphana.escience.pure.generated.model.ClassificationRef;
import de.leuphana.escience.pure.generated.model.CompoundDate;
import de.leuphana.escience.pure.generated.model.DoiElectronicVersion;
import de.leuphana.escience.pure.generated.model.Organization;
import de.leuphana.escience.pure.generated.model.PublicationStatus;
import de.leuphana.escience.pure.generated.model.ResearchOutput;
import de.leuphana.escience.pure.generated.model.Visibility;
import de.leuphana.escience.export.identifiers.Identifiers;
import de.leuphana.escience.pure.imports.DSpaceObjectMappings;
import de.leuphana.escience.pure.DSpaceServicesContainer;
import de.leuphana.escience.pure.apiobjects.PureWSResultItem;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

public class ResearchOutputExport extends AbstractExport {

    private static final Logger log = LoggerFactory.getLogger(ResearchOutputExport.class);

    private ResearchOutputApi researchOutputApi;
    protected Map<ResearchOutputMappingType, Map<String, String>> mapping = new HashMap<>();
    private final ExportStatus exportStatus;
    protected static final String mappingPropertiesPrefix =
        generalMappingPropertiesPrefix + "." + ExportType.RESEARCH_OUTPUT.getMappingSuffix();

    public ResearchOutputExport(String leuphanaPureWsEndpointBase, String leuphanaPureWsApiKey,
                                DSpaceServicesContainer dSpaceServicesContainer,
                                DSpaceObjectMappings dSpaceObjectMappings, ExportStatus exportStatus,
                                RestTemplate duplicateCheckRestTemplate) {
        super(leuphanaPureWsEndpointBase, leuphanaPureWsApiKey, dSpaceServicesContainer, dSpaceObjectMappings,
              exportStatus, duplicateCheckRestTemplate);
        this.exportStatus = exportStatus;
    }

    @Override
    protected void setupClassifications() throws ApiException {
        log.info("Fetch and store Pure Research Output classifications...");
        for (ResearchOutputMappingType mappingType : ResearchOutputMappingType.values()) {
            log.info("Fetching Pure classification {}", mappingType);
            mappingType.getClassificationRefs(researchOutputApi);
        }
    }

    @Override
    public void init() throws ApiException {
        super.init();
        setupMappingFromConfiguration();
        researchOutputApi = createResearchOutputApiClient();
        setupClassifications();
    }

    ResearchOutputApi createResearchOutputApiClient() {
        return new ResearchOutputApi(apiClient);
    }


    ClassificationRef getClassificationRefForMetadataValuesFromMapping(List<MetadataValue> metadataValues,
                                                                       ResearchOutputMappingType mappingType)
        throws ApiException {
        if (!metadataValues.isEmpty()) {
            for (MetadataValue metadataValue : metadataValues) {
                ClassificationRef c = getClassificationRefForMetadataValueFromMapping(metadataValue.getValue(),
                                                                                      mappingType);
                if (c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    ClassificationRef getClassificationRefForMetadataValueFromMapping(String metadataValue,
                                                                      ResearchOutputMappingType mappingType)
        throws ApiException {
        return getClassificationRefFromListForMetadataValue(metadataValue, mapping.get(mappingType),
                                                            mappingType.getClassificationRefs(researchOutputApi));
    }

    @Override
    public ExportItem createExport(Context context, Item item,
                                   String syncTypeValue) throws ApiException, SQLException {

        List<MetadataValue> creationContextMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "local.CreationContext");
        String publicationLanguage =
            dSpaceServicesContainer.getItemService()
                                   .getMetadataFirstValue(item, "DataCite", "Language", null, Item.ANY);
        List<MetadataValue> publicationYearMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "DataCite.PublicationYear");
        List<MetadataValue> titleMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "dc.title");
        List<MetadataValue> subTitleMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "DataCite.Title.Subtitle");
        List<MetadataValue> abstractMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "DataCite.Description.Abstract");
        List<MetadataValue> identifierMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "dc.identifier.uri");
        List<MetadataValue> affiliationMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "local.Affiliation");

        String doi = null;
        for (MetadataValue metadataValue : identifierMetadataValues) {
            if (metadataValue.getValue().startsWith(Identifiers.DOI_RESOLVER_HTTPS)) {
                doi = metadataValue.getValue();
                break;
            }
        }

        ClassificationRef research =
            getClassificationRefForMetadataValuesFromMapping(creationContextMetadataValues,
                                                             ResearchOutputMappingType.CREATION_CONTEXT);
        ClassificationRef type = getClassificationRefForMetadataValueFromMapping(syncTypeValue,
                                                                                 ResearchOutputMappingType.TYPE);
        ClassificationRef published =
            getClassificationRefForMetadataValueFromMapping(DSpaceToPure.DEFAULT_METADATA_VALUE,
                                                            ResearchOutputMappingType.PUBLISHED);
        ClassificationRef language =
            getClassificationRefForMetadataValueFromMapping(StringUtils.isNotEmpty(publicationLanguage) ?
                                                                publicationLanguage :
                                                                DSpaceToPure.DEFAULT_METADATA_VALUE,
                                                            ResearchOutputMappingType.LANGUAGE);

        ElectronicVersionAccessDetail electronicVersionAccessDetail =
            getElectronicVersionAccessDetail(context, item);
        if (electronicVersionAccessDetail.getAccessType() == null) {
            log.error(exportStatus.error(item, "Could not get access type!"));
            return null;
        }

        ClassificationRef accessType =
            getClassificationRefForMetadataValueFromMapping(
                String.valueOf(electronicVersionAccessDetail.getAccessType()),
                ResearchOutputMappingType.ACCESS_TYPE);

        ClassificationRef versionType =
            getClassificationRefForMetadataValueFromMapping(DSpaceToPure.DEFAULT_METADATA_VALUE,
                                                            ResearchOutputMappingType.ELECTRONIC_VERSION_TYPE);

        String license = electronicVersionAccessDetail.getLicense();
        if (!mapping.get(ResearchOutputMappingType.LICENSE).containsKey(license)) {
            license = DSpaceToPure.DEFAULT_METADATA_VALUE;
        }
        ClassificationRef licenseType =
            getClassificationRefForMetadataValueFromMapping(license, ResearchOutputMappingType.LICENSE);

        ClassificationRef authorRole =
            getClassificationRefForMetadataValueFromMapping(syncTypeValue + "-" + DSpaceToPure.PURE_AUTHOR_ROLE
                , ResearchOutputMappingType.ROLE);

        if (research == null) {
            log.error(exportStatus.error(item, "Could not get 'research' Classification!"));
            return null;
        }
        if (type == null) {
            log.error(exportStatus.error(item, "Could not get 'type' Classification!"));
            return null;
        }
        if (published == null) {
            log.error(exportStatus.error(item, "Could not get 'published' Classification!"));
            return null;
        }
        if (language == null) {
            log.error(exportStatus.error(item, "Could not get 'language' Classification!"));
            return null;
        }
        if (StringUtils.isNotEmpty(doi)) {
            if (accessType == null) {
                log.error(exportStatus.error(item, "Could not get 'accessType' Classification!"));
                return null;
            }
            if (versionType == null) {
                log.error(exportStatus.error(item, "Could not get 'versionType' Classification!"));
                return null;
            }
            if (licenseType == null) {
                log.error(exportStatus.error(item, "Could not get 'licenseType' Classification!"));
                return null;
            }
        }
        if (authorRole == null) {
            log.error(exportStatus.error(item, "Could not get 'authorRole' Classification!"));
            return null;
        }

        PublicationStatus publicationStatus = new PublicationStatus();
        publicationStatus.setPublicationDate(new CompoundDate().year(
            Integer.valueOf(publicationYearMetadataValues.get(0).getValue())));
        publicationStatus.setPublicationStatus(published);

        ResearchOutput researchOutput = new BookAnthology();

        addTitles(researchOutput, publicationLanguage, titleMetadataValues, subTitleMetadataValues,
                  abstractMetadataValues);

        researchOutput.setType(type);
        researchOutput.setCategory(research);
        researchOutput.setPublicationStatuses(List.of(publicationStatus));
        researchOutput.setLanguage(language);

        if (StringUtils.isNotEmpty(doi)) {
            DoiElectronicVersion doiElectronicVersion = new DoiElectronicVersion().doi(doi);
            doiElectronicVersion.setAccessType(accessType);
            doiElectronicVersion.setVersionType(versionType);
            doiElectronicVersion.setLicenseType(licenseType);
            researchOutput.setElectronicVersions(List.of(doiElectronicVersion));
        }

        //FIXME multiple authors for types != dissertation?
        List<Item> authors = getItemAuthors(context, item, false);
        if (authors == null) {
            return null;
        }
        addContributors(researchOutput, authors, authorRole);

        UUID organizationUUID = defaultOrganizationUUID;
        if (!affiliationMetadataValues.isEmpty() &&
            dSpaceObjectMappings.getOrganizationNameToPureMap()
                                .containsKey(affiliationMetadataValues.get(0).getValue())) {
            organizationUUID =
                dSpaceObjectMappings.getOrganizationNameToPureMap().get(affiliationMetadataValues.get(0).getValue());
        }
        Organization managingOrganization =
            new Organization(null, organizationUUID, null, null, null, null,
                             null, null, null, "Organization");
        researchOutput.setManagingOrganization(managingOrganization);
        researchOutput.setOrganizations(List.of(managingOrganization));
        researchOutput.setVisibility(new Visibility().key(Visibility.KeyEnum.FREE));

        return new ExportItem(researchOutput);
    }

    @Override
    public ExportResult export(ExportItem exportItem) throws ApiException {
        ExportResult exportResult = new ExportResult();
        ResearchOutput researchOutput = researchOutputApi.researchOutputCreate((ResearchOutput) exportItem.export());
        if (researchOutput != null) {
            exportResult.setUuid(researchOutput.getUuid());
            exportResult.setPortalUrl(researchOutput.getPortalUrl());
        }
        return exportResult;
    }

    @Override
    protected DuplicateCheckResult concreteDuplicateCheck(PureWSResultItem pureWSResultItem, String doi, String title) {
        log.warn("Duplicate check not yet implemented!");
        return DuplicateCheckResult.NO_DUPLICATE;
    }

    @Override
    protected void setupMappingFromConfiguration() {
        if (mapping.isEmpty()) {
            for (ResearchOutputMappingType mappingType : ResearchOutputMappingType.values()) {
                mapping.put(mappingType, new HashMap<>());
            }
        }
        Pattern propertyKeyMappingTypePattern = Pattern.compile(
            "^" + ResearchOutputExport.mappingPropertiesPrefix.replaceAll("\\.", "\\\\.") + "\\.([^.]+)\\.([^.]+)$");
        for (String propertyKey : dSpaceServicesContainer.getConfigurationService().getPropertyKeys(
            ResearchOutputExport.mappingPropertiesPrefix)) {
            log.info("Registering mapping Property: {}", propertyKey);
            Matcher matcher = propertyKeyMappingTypePattern.matcher(propertyKey);
            if (matcher.matches()) {
                String mappingTypeCandidate = matcher.group(1);
                String valueToMap = matcher.group(2);
                try {
                    ResearchOutputMappingType mappingType =
                        ResearchOutputMappingType.valueOf(mappingTypeCandidate.toUpperCase());
                    mapping.get(mappingType)
                           .put(valueToMap, dSpaceServicesContainer.getConfigurationService().getProperty(propertyKey));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Mapping type '" + mappingTypeCandidate + "' not valid!", e);
                }
            } else {
                throw new IllegalArgumentException("Property key '" + propertyKey + "' not valid!");
            }
        }
    }
}
