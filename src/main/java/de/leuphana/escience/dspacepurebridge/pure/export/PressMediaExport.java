package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.Constants;
import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.PressMediaApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.*;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpaceObjectMappings;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

class PressMediaExport extends AbstractExport {

    private static final Logger log = LoggerFactory.getLogger(PressMediaExport.class);
    private final ExportStatus exportStatus;

    private PressMediaApi pressMediaApi;
    protected Map<PressMediaMappingType, Map<String, String>> mapping = new EnumMap<>(PressMediaMappingType.class);

    protected static final String MAPPING_PROPERTIES_PREFIX =
        GENERAL_MAPPING_PROPERTIES_PREFIX + "." + ExportType.PRESS_MEDIA.getMappingSuffix();
    protected static final String METADATA_PROPERTY_PREFIX = "dspace-pure-bridge.export.metadata.pressMedia.";

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PressMediaExport(String leuphanaPureWsEndpointBase, String leuphanaPureWsApiKey,
                            DSpaceServicesContainer dSpaceServicesContainer,
                            DSpaceObjectMappings dSpaceObjectMappings, ExportStatus exportStatus,
                            RestTemplate duplicateCheckRestTemplate) {
        super(leuphanaPureWsEndpointBase, leuphanaPureWsApiKey, dSpaceServicesContainer, dSpaceObjectMappings,
            exportStatus, duplicateCheckRestTemplate);
        this.exportStatus = exportStatus;
    }

    @Override
    public void init() throws ApiException {
        super.init();
        setupMappingFromConfiguration();
        pressMediaApi = createPressMediaApiClient();
        setupClassifications();
    }

    PressMediaApi createPressMediaApiClient() {
        return new PressMediaApi(apiClient);
    }

    @Override
    public void setupClassifications() throws ApiException {
        log.info("Fetch and store Pure Press/Media classifications...");
        for (PressMediaMappingType mappingType : PressMediaMappingType.values()) {
            log.info("Fetching Pure classification {}", mappingType);
            mappingType.getClassificationRefs(pressMediaApi);
        }
    }

    @Override
    public ExportItem createExport(Context context, Item item, String syncTypeValue)
        throws ApiException, SQLException {
        ConfigurationService configurationService = dSpaceServicesContainer.getConfigurationService();

        List<MetadataValue> titleMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "title", "dc.title"));
        List<MetadataValue> coverageDateMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "coverageDate", "dc.date.issued"));
        List<MetadataValue> affiliationMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "managingOrganization", "local.Affiliation"));
        List<MetadataValue> descriptionMetadataValues =
                dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                        configurationService.getProperty(METADATA_PROPERTY_PREFIX + "description",
                                "DataCite.Description.Abstract"));

        ClassificationRef type = getClassificationRefForMetadataValueFromMapping(syncTypeValue,
            PressMediaMappingType.TYPE);
        ClassificationRef personRole =
            getClassificationRefForMetadataValueFromMapping(DSpaceToPure.PURE_AUTHOR_ROLE,
                PressMediaMappingType.CONTRIBUTOR_ROLE);

        if (titleMetadataValues.isEmpty()) {
            log.error(exportStatus.error(item, "Could not get 'title'!"));
            return null;
        }
        if (type == null) {
            log.error(exportStatus.error(item, "Could not get 'type' Classification!"));
            return null;
        }
        if (personRole == null) {
            log.error(exportStatus.error(item, "Could not get 'personRole' Classification!"));
            return null;
        }

        String coverageTypeValue = configurationService.getProperty(METADATA_PROPERTY_PREFIX + "coverageType",
            MediaCoverage.CoverageTypeEnum.CONTRIBUTION.getValue());
        MediaCoverage.CoverageTypeEnum coverageType;
        try {
            coverageType = MediaCoverage.CoverageTypeEnum.fromValue(coverageTypeValue);
        } catch (IllegalArgumentException e) {
            log.error(exportStatus.error(item, "Configured coverageType '" + coverageTypeValue + "' is not valid!"));
            return null;
        }

        LocalDate coverageDate = getCoverageDate(coverageDateMetadataValues);
        if (coverageDate == null) {
            log.error(exportStatus.error(item, "Could not get mandatory MediaCoverage 'date'!"));
            return null;
        }

        List<Item> authors = getItemAuthors(context, item, true);

        Map<String, String> localizedTitle = buildLocalizedValues(titleMetadataValues);

        PressMedia pressMedia = new PressMedia();
        pressMedia.setTitle(localizedTitle);
        pressMedia.setType(type);
        pressMedia.setVisibility(new Visibility().key(Visibility.KeyEnum.FREE));

        UUID organizationUUID = defaultOrganizationUUID;
        if (!affiliationMetadataValues.isEmpty() &&
            dSpaceObjectMappings.getOrganizationNameToPureMap()
                .containsKey(affiliationMetadataValues.get(0).getValue())) {
            organizationUUID =
                dSpaceObjectMappings.getOrganizationNameToPureMap().get(affiliationMetadataValues.get(0).getValue());
        }
        OrganizationRef managingOrganization = new OrganizationRef();
        managingOrganization.setUuid(organizationUUID);
        managingOrganization.setSystemName("Organization");
        pressMedia.setManagingOrganization(managingOrganization);

        MediaCoverage mediaCoverage = new MediaCoverage();
        mediaCoverage.setTitle(localizedTitle);
        mediaCoverage.setCoverageType(coverageType);
        mediaCoverage.setDate(coverageDate);
        mediaCoverage.setPersons(buildPersons(authors, personRole));
        if (!descriptionMetadataValues.isEmpty()) {
            mediaCoverage.setDescription(buildLocalizedValues(descriptionMetadataValues));
        }
        pressMedia.setMediaCoverages(List.of(mediaCoverage));

        return new ExportItem(pressMedia);
    }

    /**
     * Builds a localized value map (e.g. for title or description). Every metadata value is stored under the Pure locale
     * that corresponds to its metadata language; values without a mappable language fall back to the German locale.
     */
    Map<String, String> buildLocalizedValues(List<MetadataValue> metadataValues) {
        Map<String, String> localizedValues = new HashMap<>();
        for (MetadataValue metadataValue : metadataValues) {
            String locale = languageDSpaceMap.containsKey(metadataValue.getLanguage())
                ? languageDSpaceMap.get(metadataValue.getLanguage()).getLocale()
                : DSpaceLanguage.GERMAN.getLocale();
            localizedValues.putIfAbsent(locale, escapeHtml4(metadataValue.getValue()));
        }
        return localizedValues;
    }

    List<AbstractPressMediaPersonAssociation> buildPersons(List<Item> authors, ClassificationRef personRole) {
        List<AbstractPressMediaPersonAssociation> persons = new ArrayList<>();
        if (authors == null || authors.isEmpty()) {
            InternalPressMediaPersonAssociation personAssociation = new InternalPressMediaPersonAssociation();
            PersonRef personRef = new PersonRef();
            personRef.setUuid(defaultAuthorUUID);
            personRef.setSystemName("Person");
            personAssociation.setPerson(personRef);
            personAssociation.setName(new Name().firstName(defaultAuthorFirstName).lastName(defaultAuthorLastName));
            personAssociation.setRole(personRole);
            persons.add(personAssociation);
        } else {
            for (Item author : authors) {
                InternalPressMediaPersonAssociation personAssociation = new InternalPressMediaPersonAssociation();
                personAssociation.setPerson(getPersonRefFromDSpacePerson(author));
                personAssociation.setName(getPersonNameFromDSpacePerson(author));
                personAssociation.setRole(personRole);
                persons.add(personAssociation);
            }
        }
        return persons;
    }

    PersonRef getPersonRefFromDSpacePerson(Item person) {
        String authorUUID = dSpaceServicesContainer.getItemService()
            .getMetadataFirstValue(person, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER, Item.ANY);
        PersonRef personRef = new PersonRef();
        personRef.setUuid(UUID.fromString(authorUUID));
        personRef.setSystemName("Person");
        return personRef;
    }

    LocalDate getCoverageDate(List<MetadataValue> coverageDateMetadataValues) {
        if (coverageDateMetadataValues.isEmpty()) {
            return null;
        }
        String value = coverageDateMetadataValues.get(0).getValue();
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (value.length() == 4) {
            return LocalDate.of(Integer.parseInt(value), 1, 1);
        }
        return LocalDate.parse(value, dateFormatter);
    }

    ClassificationRef getClassificationRefForMetadataValueFromMapping(String metadataValue,
                                                                      PressMediaMappingType mappingType)
        throws ApiException {
        return getClassificationRefFromListForMetadataValue(metadataValue, mapping.get(mappingType),
            mappingType.getClassificationRefs(pressMediaApi));
    }

    @Override
    public ExportResult export(ExportItem exportItem) throws ApiException {
        ExportResult exportResult = new ExportResult();
        PressMedia pressMedia = pressMediaApi.pressmediaCreate((PressMedia) exportItem.export());
        if (pressMedia != null) {
            exportResult.setUuid(pressMedia.getUuid());
            exportResult.setPortalUrl(pressMedia.getPortalUrl());
        }
        return exportResult;
    }

    // Duplicate check is stubbed for now (analogue to ResearchOutputExport); PRESS_MEDIA has no search result class,
    // therefore AbstractExport.checkForDuplicate skips and this method is not reached during a regular export.
    @Override
    public DuplicateCheckResult concreteDuplicateCheck(PureWSResultItem pureWSResultItem, String doi, String title) {
        log.warn("Duplicate check not yet implemented!");
        return DuplicateCheckResult.NO_DUPLICATE;
    }

    @Override
    public void setupMappingFromConfiguration() {
        if (mapping.isEmpty()) {
            for (PressMediaMappingType mappingType : PressMediaMappingType.values()) {
                mapping.put(mappingType, new HashMap<>());
            }
        }
        Pattern propertyKeyMappingTypePattern = Pattern.compile(
            "^" + PressMediaExport.MAPPING_PROPERTIES_PREFIX.replaceAll("\\.", "\\\\.") + "\\.([^.]+)\\.([^.]+)$");
        for (String propertyKey : dSpaceServicesContainer.getConfigurationService().getPropertyKeys(
            PressMediaExport.MAPPING_PROPERTIES_PREFIX)) {
            log.info("Registering mapping Property: {}", propertyKey);
            Matcher matcher = propertyKeyMappingTypePattern.matcher(propertyKey);
            if (matcher.matches()) {
                String mappingTypeCandidate = matcher.group(1);
                String valueToMap = matcher.group(2);
                try {
                    PressMediaMappingType mappingType =
                        PressMediaMappingType.valueOf(mappingTypeCandidate.toUpperCase());
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