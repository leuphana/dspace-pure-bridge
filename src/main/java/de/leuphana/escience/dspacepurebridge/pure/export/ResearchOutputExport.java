package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.identifiers.Identifiers;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.AccessType;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.ResearchOutputApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.*;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpaceObjectMappings;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ResearchOutputExport extends AbstractExport {

    private static final Logger log = LoggerFactory.getLogger(ResearchOutputExport.class);

    private ResearchOutputApi researchOutputApi;
    protected Map<ResearchOutputMappingType, Map<String, String>> mapping =
        new EnumMap<>(ResearchOutputMappingType.class);
    private final ExportStatus exportStatus;
    protected static final String MAPPING_PROPERTIES_PREFIX =
        GENERAL_MAPPING_PROPERTIES_PREFIX + "." + ExportType.RESEARCH_OUTPUT.getMappingSuffix();

    protected static final String METADATA_PROPERTY_PREFIX = "dspace-pure-bridge.export.metadata.researchOutput.";

    public ResearchOutputExport(String leuphanaPureWsEndpointBase, String leuphanaPureWsApiKey,
                                DSpaceServicesContainer dSpaceServicesContainer,
                                DSpaceObjectMappings dSpaceObjectMappings, ExportStatus exportStatus,
                                RestTemplate duplicateCheckRestTemplate) {
        super(leuphanaPureWsEndpointBase, leuphanaPureWsApiKey, dSpaceServicesContainer, dSpaceObjectMappings,
            exportStatus, duplicateCheckRestTemplate);
        this.exportStatus = exportStatus;
    }

    @Override
    public void setupClassifications() throws ApiException {
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
        ConfigurationService configurationService = dSpaceServicesContainer.getConfigurationService();

        List<MetadataValue> creationContextMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "category", "local.CreationContext"));

        List<MetadataValue> publicationLanguages = dSpaceServicesContainer.getItemService().getMetadataByMetadataString(
            item, configurationService.getProperty(METADATA_PROPERTY_PREFIX + "language", "DataCite.Language"));
        String publicationLanguage = publicationLanguages.isEmpty() ? null : publicationLanguages.get(0).getValue();

        List<MetadataValue> publicationYearMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "publicationYear",
                    "DataCite.PublicationYear"));

        List<MetadataValue> titleMetadataValues =
            dSpaceServicesContainer.getItemService()
                .getMetadataByMetadataString(item,
                    configurationService.getProperty(METADATA_PROPERTY_PREFIX + "title", "dc.title"));
        List<MetadataValue> subTitleMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "subTitle", "DataCite.Title.Subtitle"));
        List<MetadataValue> abstractMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "abstract",
                    "DataCite.Description.Abstract"));
        List<MetadataValue> identifierMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "dc.identifier.uri");
        List<MetadataValue> affiliationMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "managingOrganization",
                    "local.Affiliation"));
        String doi = null;
        for (MetadataValue metadataValue : identifierMetadataValues) {
            if (metadataValue.getValue().startsWith(Identifiers.DOI_RESOLVER_HTTPS)) {
                doi = metadataValue.getValue();
                break;
            }
        }

        ClassificationRef category =
            getClassificationRefForMetadataValuesFromMapping(creationContextMetadataValues,
                ResearchOutputMappingType.CATEGORY);
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
            getClassificationRefForMetadataValueFromMapping(DSpaceToPure.PURE_AUTHOR_ROLE
                , ResearchOutputMappingType.ROLE);

        if (category == null) {
            log.error(exportStatus.error(item, "Could not get 'category' Classification!"));
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
        researchOutput.setCategory(category);
        researchOutput.setPublicationStatuses(List.of(publicationStatus));
        researchOutput.setLanguage(language);

        if (StringUtils.isNotEmpty(doi)) {
            DoiElectronicVersion doiElectronicVersion = new DoiElectronicVersion().doi(doi);
            doiElectronicVersion.setAccessType(accessType);
            doiElectronicVersion.setVersionType(versionType);
            doiElectronicVersion.setLicenseType(licenseType);
            researchOutput.setElectronicVersions(List.of(doiElectronicVersion));
        }

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
    public DuplicateCheckResult concreteDuplicateCheck(PureWSResultItem pureWSResultItem, String doi, String title) {
        log.warn("Duplicate check not yet implemented!");
        return DuplicateCheckResult.NO_DUPLICATE;
    }

    @Override
    public void setupMappingFromConfiguration() {
        if (mapping.isEmpty()) {
            for (ResearchOutputMappingType mappingType : ResearchOutputMappingType.values()) {
                mapping.put(mappingType, new HashMap<>());
            }
        }
        Pattern propertyKeyMappingTypePattern = Pattern.compile(
            "^" + ResearchOutputExport.MAPPING_PROPERTIES_PREFIX.replaceAll("\\.", "\\\\.") + "\\.([^.]+)\\.([^.]+)$");
        for (String propertyKey : dSpaceServicesContainer.getConfigurationService().getPropertyKeys(
            ResearchOutputExport.MAPPING_PROPERTIES_PREFIX)) {
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

    ElectronicVersionAccessDetail getElectronicVersionAccessDetail(Context context, Item item) throws SQLException {

        List<Bundle> bnds;
        try {
            bnds = dSpaceServicesContainer.getItemService().getBundles(item,
                org.dspace.core.Constants.DEFAULT_BUNDLE_NAME);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        ElectronicVersionAccessDetail electronicVersionAccessDetail = new ElectronicVersionAccessDetail();
        String licenseMetadataField = dSpaceServicesContainer.getConfigurationService()
            .getProperty(METADATA_PROPERTY_PREFIX + "bitstreamLicense", "local.BitstreamLicense");

        if (bnds != null) {
            for (Bundle bnd : bnds) {
                Bitstream bitstream = bnd.getPrimaryBitstream();
                if (bitstream != null) {
                    AccessType accessType = getAccessRightsValueForPolicies(context,
                        dSpaceServicesContainer
                            .getAuthorizeService()
                            .getPoliciesActionFilter(
                                context,
                                bitstream,
                                org.dspace.core.Constants.READ));
                    electronicVersionAccessDetail.setAccessType(accessType);

                    List<MetadataValue> licenseMetadataValues = dSpaceServicesContainer.getBitstreamService()
                        .getMetadataByMetadataString(bitstream, licenseMetadataField);
                    String license = licenseMetadataValues.isEmpty() ? null : licenseMetadataValues.get(0).getValue();
                    electronicVersionAccessDetail.setAccessType(accessType);
                    electronicVersionAccessDetail.setLicense(license);
                } else {
                    for (Bitstream b : bnd.getBitstreams()) {
                        AccessType accessType = getAccessRightsValueForPolicies(context, dSpaceServicesContainer
                            .getAuthorizeService()
                            .getPoliciesActionFilter(
                                context, b,
                                org.dspace.core.Constants.READ));
                        List<MetadataValue> licenseMetadataValues = dSpaceServicesContainer.getBitstreamService()
                            .getMetadataByMetadataString(b, licenseMetadataField);
                        String license =
                            licenseMetadataValues.isEmpty() ? null : licenseMetadataValues.get(0).getValue();
                        if (AccessType.EMBARGO.equals(accessType)) {
                            electronicVersionAccessDetail.setAccessType(accessType);
                            electronicVersionAccessDetail.setLicense(license);
                            break;
                        } else {
                            if (electronicVersionAccessDetail.getAccessType() == null) {
                                electronicVersionAccessDetail.setAccessType(accessType);
                            }
                            if (electronicVersionAccessDetail.getLicense() == null) {
                                electronicVersionAccessDetail.setLicense(license);
                            }
                        }
                    }
                }
            }
        }
        return electronicVersionAccessDetail;
    }

    AccessType getAccessRightsValueForPolicies(Context context, List<ResourcePolicy> rps) throws SQLException {

        AccessType accessType = null;
        Date now = new Date();

        if (rps != null) {
            for (ResourcePolicy rp : rps) {
                if (rp.getGroup() != null && Group.ANONYMOUS.equals(rp.getGroup().getName())) {
                    if (dSpaceServicesContainer.getResourcePolicyService().isDateValid(rp) && accessType == null) {
                        accessType = AccessType.OPEN_ACCESS;
                    } else if (rp.getStartDate() != null && rp.getStartDate().after(now)) {
                        accessType = AccessType.EMBARGO;
                    }
                } else if (rp.getGroup() != null && !Group.ADMIN.equals(rp.getGroup().getName())) {
                    if (dSpaceServicesContainer.getResourcePolicyService().isDateValid(rp) && accessType == null) {
                        accessType = AccessType.RESTRICTED;
                    } else if (rp.getStartDate() == null || rp.getStartDate().after(now)) {
                        accessType = AccessType.EMBARGO;
                    }
                }
                context.uncacheEntity(rp);
            }
        }
        return accessType;
    }


}
