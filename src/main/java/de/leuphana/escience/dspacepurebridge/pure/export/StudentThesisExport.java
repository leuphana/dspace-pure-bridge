package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultStudentThesisItem;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.StudentThesisApi;
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

class StudentThesisExport extends AbstractExport {
    private static final Logger log = LoggerFactory.getLogger(StudentThesisExport.class);
    private final ExportStatus exportStatus;

    private StudentThesisApi studentThesisApi;
    protected Map<StudentThesisMappingType, Map<String, String>> mapping = new EnumMap<>(StudentThesisMappingType.class);
    protected static final String MAPPING_PROPERTIES_PREFIX =
        GENERAL_MAPPING_PROPERTIES_PREFIX + "." + ExportType.STUDENT_THESIS.getMappingSuffix();
    protected static final String METADATA_PROPERTY_PREFIX = "dspace-pure-bridge.export.metadata.studentThesis.";

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StudentThesisExport(String leuphanaPureWsEndpointBase, String leuphanaPureWsApiKey,
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
        studentThesisApi = createStudentThesisApiClient();
        setupClassifications();
    }

    @Override
    public ExportItem createExport(Context context, Item item, String syncTypeValue) throws ApiException, SQLException {
        ConfigurationService configurationService = dSpaceServicesContainer.getConfigurationService();

        List<MetadataValue> publicationLanguages = dSpaceServicesContainer.getItemService().getMetadataByMetadataString(
            item, configurationService.getProperty(METADATA_PROPERTY_PREFIX + "language", "DataCite.Language"));
        String publicationLanguage = publicationLanguages.isEmpty() ? null : publicationLanguages.get(0).getValue();
        List<MetadataValue> awardDateList = dSpaceServicesContainer.getItemService().getMetadataByMetadataString(
            item,
            configurationService.getProperty(METADATA_PROPERTY_PREFIX + "awardDate", "dc.date.accepted"));
        String awardDate = awardDateList.isEmpty() ? null : awardDateList.get(0).getValue();
        List<MetadataValue> titleMetadataValues =
            dSpaceServicesContainer.getItemService()
                .getMetadataByMetadataString(item,
                    configurationService.getProperty(METADATA_PROPERTY_PREFIX + "title", "dc.title"));
        List<MetadataValue> subTitleMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "subTitle", "DataCite.Title.Subtitle"));
        List<MetadataValue> abstractMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "abstract", "DataCite.Description.Abstract"));
        List<MetadataValue> identifierMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "dc.identifier.uri");
        List<MetadataValue> managingOrganizationMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "managingOrganization", "local.Affiliation"));
        List<MetadataValue> awardingInstitutionMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item,
                configurationService.getProperty(METADATA_PROPERTY_PREFIX + "awardingInstitution", "dc.contributor.grantor"));

        ClassificationRef type = getClassificationRefForMetadataValueFromMapping(syncTypeValue,
            StudentThesisMappingType.TYPE);

        ClassificationRef language =
            getClassificationRefForMetadataValueFromMapping(StringUtils.isNotEmpty(publicationLanguage) ?
                    publicationLanguage :
                    DSpaceToPure.DEFAULT_METADATA_VALUE,
                StudentThesisMappingType.LANGUAGE);

        ClassificationRef authorRole =
            getClassificationRefForMetadataValueFromMapping(DSpaceToPure.PURE_AUTHOR_ROLE
                , StudentThesisMappingType.CONTRIBUTOR_ROLE);

        ClassificationRef supervisorRole =
            getClassificationRefForMetadataValueFromMapping(DSpaceToPure.PURE_SUPERVISOR_ROLE,
                StudentThesisMappingType.SUPERVISOR_ROLE);

        ClassificationRef reviewerRole =
            getClassificationRefForMetadataValueFromMapping(DSpaceToPure.PURE_REVIEWER_ROLE,
                StudentThesisMappingType.SUPERVISOR_ROLE);

        if (type == null) {
            log.error(exportStatus.error(item, "Could not get 'type' Classification!"));
            return null;
        }
        if (language == null) {
            log.error(exportStatus.error(item, "Could not get 'language' Classification!"));
            return null;
        }
        if (authorRole == null) {
            log.error(exportStatus.error(item, "Could not get 'authorRole' Classification!"));
            return null;
        }
        if (supervisorRole == null) {
            log.error(exportStatus.error(item, "Could not get 'supervisorRole' Classification!"));
            return null;
        }
        if (reviewerRole == null) {
            log.error(exportStatus.error(item, "Could not get 'reviewerRole' Classification!"));
            return null;
        }

        StudentThesis studentThesis = new StudentThesis();
        studentThesis.setSystemName("StudentThesis");

        Visibility visibility = new Visibility();
        visibility.setKey(Visibility.KeyEnum.FREE);
        studentThesis.setVisibility(visibility);

        addTitles(studentThesis, publicationLanguage, titleMetadataValues, subTitleMetadataValues,
            abstractMetadataValues);

        studentThesis.setLanguage(language);
        studentThesis.setType(type);

        List<Link> links = new ArrayList<>();
        for (MetadataValue identifierMetadataValue : identifierMetadataValues) {
            Link link = new Link();
            link.setUrl(identifierMetadataValue.getValue());
            links.add(link);
        }

        studentThesis.setLinks(links);

        List<Item> supervisors = getItemAdvisors(context, item);
        List<Item> referees = getItemReferees(context, item);
        List<Item> authors = getItemAuthors(context, item, !supervisors.isEmpty() || !referees.isEmpty());
        if (authors == null) {
            return null;
        }
        addContributors(studentThesis, authors, authorRole);
        addSupervisors(studentThesis, supervisors, supervisorRole);
        addSupervisors(studentThesis, referees, reviewerRole);

        if (awardDate != null) {
            CompoundDate compoundDate = new CompoundDate();
            if (awardDate.length() == 4) {
                compoundDate.setYear(Integer.valueOf(awardDate));
            } else {
                LocalDate date = LocalDate.parse(awardDate, dateFormatter);
                compoundDate.setDay(date.getDayOfMonth());
                compoundDate.setMonth(date.getMonthValue());
                compoundDate.setYear(date.getYear());
            }
            studentThesis.setAwardDate(compoundDate);
        }

        UUID affiliationOrgId = defaultOrganizationUUID;
        UUID grantorOrgId = defaultOrganizationUUID;
        if (!managingOrganizationMetadataValues.isEmpty() &&
            dSpaceObjectMappings.getOrganizationNameToPureMap()
                .containsKey(managingOrganizationMetadataValues.get(0).getValue())) {
            affiliationOrgId =
                dSpaceObjectMappings.getOrganizationNameToPureMap().get(managingOrganizationMetadataValues.get(0).getValue());
        }
        if (!awardingInstitutionMetadataValues.isEmpty() &&
            dSpaceObjectMappings.getOrganizationNameToPureMap().containsKey(awardingInstitutionMetadataValues.get(0).getValue())) {
            grantorOrgId =
                dSpaceObjectMappings.getOrganizationNameToPureMap().get(awardingInstitutionMetadataValues.get(0).getValue());
        }

        Organization awardingOrganization = new Organization(null, grantorOrgId, null, null, null, null,
            null, null, null, "Organization");
        OrganizationOrExternalOrganizationRef awardingOrganizationOrExternalOrganizationRef =
            new OrganizationOrExternalOrganizationRef();
        awardingOrganizationOrExternalOrganizationRef.setOrganizationRef(awardingOrganization);
        studentThesis.setAwardingInstitutions(List.of(awardingOrganizationOrExternalOrganizationRef));

        Organization affiliationOrganization = new Organization(
            null, affiliationOrgId, null, null, null, null,
            null, null, null, "Organization");
        studentThesis.addOrganizationsItem(affiliationOrganization);
        studentThesis.setManagingOrganization(affiliationOrganization);

        return new ExportItem(studentThesis);
    }

    void addSupervisors(StudentThesis studentThesis, List<Item> supervisors, ClassificationRef supervisorRole) {
        for (Item supervisor : supervisors) {
            SupervisorAssociation supervisorAssociation = new SupervisorAssociation();
            supervisorAssociation.setPerson(getAPIPersonFromDSpacePerson(supervisor));
            supervisorAssociation.setName(getPersonNameFromDSpacePerson(supervisor));
            supervisorAssociation.setRole(supervisorRole);
            studentThesis.addSupervisorsItem(supervisorAssociation);
        }
    }

    @Override
    public void setupMappingFromConfiguration() {
        if (mapping.isEmpty()) {
            for (StudentThesisMappingType mappingType : StudentThesisMappingType.values()) {
                mapping.put(mappingType, new HashMap<>());
            }
        }
        Pattern propertyKeyMappingTypePattern =
            Pattern.compile("^" + StudentThesisExport.MAPPING_PROPERTIES_PREFIX.replaceAll("\\.", "\\\\.") + "\\." +
                "([^.]+)\\.([^.]+)$");
        for (String propertyKey :
            dSpaceServicesContainer.getConfigurationService()
                .getPropertyKeys(StudentThesisExport.MAPPING_PROPERTIES_PREFIX)) {
            log.info("Registering mapping Property: {}", propertyKey);
            Matcher matcher = propertyKeyMappingTypePattern.matcher(propertyKey);
            if (matcher.matches()) {
                String mappingTypeCandidate = matcher.group(1);
                String valueToMap = matcher.group(2);
                try {
                    StudentThesisMappingType mappingType =
                        StudentThesisMappingType.valueOf(mappingTypeCandidate.toUpperCase());
                    mapping.get(mappingType).put(valueToMap,
                        dSpaceServicesContainer.getConfigurationService()
                            .getProperty(propertyKey));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Mapping type '" + mappingTypeCandidate + "' not valid!", e);
                }
            } else {
                throw new IllegalArgumentException("Property key '" + propertyKey + "' not valid!");
            }
        }
    }

    ClassificationRef getClassificationRefForMetadataValuesFromMapping(List<MetadataValue> metadataValues,
                                                                       StudentThesisMappingType mappingType)
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
                                                                      StudentThesisMappingType mappingType)
        throws ApiException {
        return getClassificationRefFromListForMetadataValue(metadataValue, mapping.get(mappingType),
            mappingType.getClassificationRefs(studentThesisApi));
    }


    @Override
    public void setupClassifications() throws ApiException {
        log.info("Fetch and store Pure Student Thesis classifications...");
        for (StudentThesisMappingType mappingType : StudentThesisMappingType.values()) {
            log.info("Fetching Pure classification {}", mappingType);
            mappingType.getClassificationRefs(studentThesisApi);
        }
    }

    @Override
    public ExportResult export(ExportItem exportItem) throws ApiException {
        ExportResult exportResult = new ExportResult();
        StudentThesis studentThesis = studentThesisApi.studentThesisCreate((StudentThesis) exportItem.export());
        if (studentThesis != null) {
            exportResult.setUuid(studentThesis.getUuid());
            exportResult.setPortalUrl(studentThesis.getPortalUrl());
        }
        return exportResult;
    }

    StudentThesisApi createStudentThesisApiClient() {
        return new StudentThesisApi(apiClient);
    }

    @Override
    public DuplicateCheckResult concreteDuplicateCheck(PureWSResultItem pureWSResultItem, String doi, String title) {
        PureWSResultStudentThesisItem studentThesisItem =
            (PureWSResultStudentThesisItem) pureWSResultItem;
        if (doi != null && studentThesisItem.getLinks() != null && !studentThesisItem.getLinks().isEmpty()) {
            for (de.leuphana.escience.dspacepurebridge.pure.apiobjects.Link link : studentThesisItem.getLinks()) {
                if (doi.equals(link.getUrl())) {
                    return DuplicateCheckResult.DOI_DUPLICATE;
                }
            }
        } else if (title.equals(studentThesisItem.getTitle().getValue())) {
            return DuplicateCheckResult.TITLE_DUPLICATE;
        }
        return DuplicateCheckResult.NO_DUPLICATE;
    }
}
