package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.Constants;
import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.identifiers.PrimaryIdentifier;
import de.leuphana.escience.dspacepurebridge.identifiers.PrimaryIdentifierHelper;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResults;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiClient;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.Configuration;
import de.leuphana.escience.dspacepurebridge.pure.generated.ServerConfiguration;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.*;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpaceObjectMappings;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLException;
import java.util.*;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

abstract class AbstractExport implements DSpaceExporter {
    protected String leuphanaPureWsEndpointBase;
    protected String leuphanaPureWsApiKey;
    protected DSpaceServicesContainer dSpaceServicesContainer;
    protected DSpaceObjectMappings dSpaceObjectMappings;
    private final ExportStatus exportStatus;
    protected UUID defaultOrganizationUUID;
    protected UUID defaultAuthorUUID;
    protected String defaultAuthorLastName;
    protected String defaultAuthorFirstName;
    protected final Map<String, DSpaceLanguage> languageDSpaceMap = new HashMap<>();
    protected ApiClient apiClient;
    static final String GENERAL_MAPPING_PROPERTIES_PREFIX = "dspace-pure-bridge.export.mapping";
    static final String AUTHOR_RELATION = "isAuthorOfPublication";
    static final String ADVISOR_RELATION = "isAdvisorOfPublication";
    static final String REFEREE_RELATION = "isRefereeOfPublication";
    private static final Logger log = LoggerFactory.getLogger(AbstractExport.class);
    private final RestTemplate duplicateCheckRestTemplate;


    protected AbstractExport(String leuphanaPureWsEndpointBase, String leuphanaPureWsApiKey,
                             DSpaceServicesContainer dSpaceServicesContainer,
                             DSpaceObjectMappings dSpaceObjectMappings, ExportStatus exportStatus,
                             RestTemplate duplicateCheckRestTemplate) {
        this.leuphanaPureWsEndpointBase = leuphanaPureWsEndpointBase;
        this.leuphanaPureWsApiKey = leuphanaPureWsApiKey;
        this.dSpaceServicesContainer = dSpaceServicesContainer;
        this.dSpaceObjectMappings = dSpaceObjectMappings;
        this.exportStatus = exportStatus;
        this.duplicateCheckRestTemplate = duplicateCheckRestTemplate;
    }

    List<Item> getItemPersons(Context context, Item publicationItem, String personToItemRelation,
                              boolean allowEmptyPersonList) throws SQLException {
        List<Item> personsWithoutPureId = new ArrayList<>();
        List<Item> itemPersons = new ArrayList<>();
        List<MetadataValue> isPersonOfPublicationList =
            dSpaceServicesContainer.getItemService().getMetadata(publicationItem,
                MetadataSchemaEnum.RELATION.getName(),
                personToItemRelation, null, Item.ANY);
        for (MetadataValue personRelation : isPersonOfPublicationList) {
            if (personRelation != null && StringUtils.isNotEmpty(personRelation.getValue())) {
                Item person = dSpaceServicesContainer.getItemService().findByIdOrLegacyId(context,
                    personRelation.getValue());
                if (person != null) {
                    String personPureUUID =
                        dSpaceServicesContainer
                            .getItemService()
                            .getMetadataFirstValue(person,
                                Constants.SCHEME,
                                Constants.ELEMENT,
                                Constants.UUID_QUALIFIER,
                                Item.ANY);
                    if (personPureUUID != null) {
                        itemPersons.add(person);
                    } else {
                        personsWithoutPureId.add(person);
                    }
                }
            }
        }
        if (!allowEmptyPersonList && AUTHOR_RELATION.equals(personToItemRelation)) {
            if (itemPersons.isEmpty()) {
                log.error(exportStatus.error(publicationItem, "Could not find any Author with Pure UUID!"));
                return null;
            } else if (!personsWithoutPureId.isEmpty()) {
                for (Item person : personsWithoutPureId) {
                    String personFirstName = dSpaceServicesContainer.getItemService().getMetadataFirstValue(person,
                        "person",
                        "givenName",
                        null,
                        Item.ANY);
                    String personLastName = dSpaceServicesContainer.getItemService().getMetadataFirstValue(person,
                        "person",
                        "familyName",
                        null,
                        Item.ANY);
                    log.error(
                        exportStatus.error(publicationItem, String.format("Could not find Pure UUID for Author %s, " +
                                "%s!",
                            personFirstName,
                            personLastName)));
                }
                return null;
            }
        }
        return itemPersons;
    }

    List<Item> getItemAuthors(Context context, Item publicationItem, boolean allowEmptyAuthorList) throws SQLException {
        return getItemPersons(context, publicationItem, AUTHOR_RELATION, allowEmptyAuthorList);
    }

    List<Item> getItemAdvisors(Context context, org.dspace.content.Item publicationItem) throws SQLException {
        return getItemPersons(context, publicationItem, ADVISOR_RELATION, false);
    }

    List<Item> getItemReferees(Context context, org.dspace.content.Item publicationItem) throws SQLException {
        return getItemPersons(context, publicationItem, REFEREE_RELATION, false);
    }

    @Nullable
    static ClassificationRef getClassificationRefFromListForMetadataValue(
        String metadataValue,
        Map<String, String> mapping,
        List<ClassificationRef> classificationRefList) {
        if (!metadataValue.isEmpty()) {
            if (mapping.containsKey(metadataValue)) {
                for (ClassificationRef c : classificationRefList) {
                    if (c.getUri().equals(mapping.get(metadataValue))) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    void validateExportObject(Object exportObject) {
        if (exportObject == null) {
            throw new IllegalArgumentException("exportObject must not be null");
        }
        if (!(exportObject instanceof StudentThesis || exportObject instanceof ResearchOutput)) {
            throw new IllegalArgumentException("exportObject must be an instance of StudentThesis or ResearchOutput");
        }
    }

    Name getPersonNameFromDSpacePerson(Item person) {
        String authorFirstName = dSpaceServicesContainer.getItemService()
            .getMetadataFirstValue(person, "person", "givenName", null,
                Item.ANY);
        String authorLastName = dSpaceServicesContainer.getItemService()
            .getMetadataFirstValue(person, "person", "familyName", null,
                Item.ANY);
        return new Name().firstName(authorFirstName).lastName(authorLastName);
    }

    Person getAPIPersonFromDSpacePerson(Item person) {
        String authorUUID = dSpaceServicesContainer
            .getItemService()
            .getMetadataFirstValue(person,
                Constants.SCHEME,
                Constants.ELEMENT,
                Constants.UUID_QUALIFIER,
                Item.ANY);
        return new Person(null, UUID.fromString(authorUUID), null, null, null
            , null, null, null, null, "Person");
    }

    void addTitles(Object exportObject, String publicationLanguage, List<MetadataValue> titleMetadataValues,
                   List<MetadataValue> subTitleMetadataValues, List<MetadataValue> abstractMetadataValues) {
        validateExportObject(exportObject);

        FormattedString formattedString = new FormattedString();
        formattedString.setValue(escapeHtml4(titleMetadataValues.get(0).getValue()));
        if (exportObject instanceof StudentThesis) {
            ((StudentThesis) exportObject).setTitle(formattedString);
        }
        if (exportObject instanceof ResearchOutput) {
            ((ResearchOutput) exportObject).setTitle(formattedString);
        }
        if (titleMetadataValues.size() > 1 &&
            languageDSpaceMap.containsKey(titleMetadataValues.get(1).getLanguage())) {
            DSpaceLanguage translatedLanguage = languageDSpaceMap.get(titleMetadataValues.get(1).getLanguage());
            Map<String, String> translatedTitleMap = new HashMap<>();
            translatedTitleMap.put(translatedLanguage.getLocale(), titleMetadataValues.get(1).getValue());
            if (exportObject instanceof StudentThesis) {
                ((StudentThesis) exportObject).setTranslatedTitle(translatedTitleMap);
            }
            if (exportObject instanceof ResearchOutput) {
                ((ResearchOutput) exportObject).setTranslatedTitle(translatedTitleMap);
            }
        }
        Map<String, String> translatedSubTitles = new HashMap<>();
        for (MetadataValue subTitleMetadataValue : subTitleMetadataValues) {
            if (languageDSpaceMap.containsKey(subTitleMetadataValue.getLanguage())) {
                DSpaceLanguage dSpaceLanguage = languageDSpaceMap.get(subTitleMetadataValue.getLanguage());
                if (dSpaceLanguage.getIso3Letter().equals(publicationLanguage)) {
                    FormattedString subTitleFormattedString = new FormattedString();
                    subTitleFormattedString.setValue(escapeHtml4(subTitleMetadataValue.getValue()));
                    if (exportObject instanceof StudentThesis) {
                        ((StudentThesis) exportObject).setSubTitle(subTitleFormattedString);
                    }
                    if (exportObject instanceof ResearchOutput) {
                        ((ResearchOutput) exportObject).setSubTitle(subTitleFormattedString);
                    }
                } else {
                    translatedSubTitles.put(dSpaceLanguage.getLocale(), escapeHtml4(subTitleMetadataValue.getValue()));
                }
            }
        }
        if (exportObject instanceof StudentThesis) {
            ((StudentThesis) exportObject).setTranslatedSubTitle(translatedSubTitles);
        }
        if (exportObject instanceof ResearchOutput) {
            ((ResearchOutput) exportObject).setTranslatedSubTitle(translatedSubTitles);
        }

        Map<String, String> abstracts = new HashMap<>();
        for (MetadataValue abstractMetadataValue : abstractMetadataValues) {
            if (languageDSpaceMap.containsKey(abstractMetadataValue.getLanguage())) {
                DSpaceLanguage dSpaceLanguage = languageDSpaceMap.get(abstractMetadataValue.getLanguage());
                abstracts.put(dSpaceLanguage.getLocale(), escapeHtml4(abstractMetadataValue.getValue()));
            }
        }
        if (exportObject instanceof StudentThesis studentThesis) {
            studentThesis.setAbstract(abstracts);
        }
        if (exportObject instanceof ResearchOutput researchOutput) {
            researchOutput.setAbstract(abstracts);
        }
    }

    void addContributors(Object exportObject, List<Item> authors, ClassificationRef authorRole) {
        validateExportObject(exportObject);
        List<AbstractContributorAssociation> authorAssociations = new ArrayList<>();
        if (authors.isEmpty()) {
            InternalContributorAssociation contributorAssociation = new InternalContributorAssociation();
            contributorAssociation.setPerson(
                    new Person(null, defaultAuthorUUID, null, null, null
                            , null, null, null, null, "Person")
            );
            contributorAssociation.setName(
                    new Name().firstName(defaultAuthorFirstName).lastName(defaultAuthorLastName));
            contributorAssociation.setRole(authorRole);
            authorAssociations.add(contributorAssociation);
        } else {
            for (Item author : authors) {
                InternalContributorAssociation contributorAssociation = new InternalContributorAssociation();
                contributorAssociation.setPerson(getAPIPersonFromDSpacePerson(author));
                contributorAssociation.setName(getPersonNameFromDSpacePerson(author));
                contributorAssociation.setRole(authorRole);
                authorAssociations.add(contributorAssociation);
            }
        }
        if (exportObject instanceof StudentThesis) {
            ((StudentThesis) exportObject).setContributors(authorAssociations);
        }
        if (exportObject instanceof ResearchOutput) {
            ((ResearchOutput) exportObject).setContributors(authorAssociations);
        }
    }

    HttpEntity<Map<String, String>> createTitleSearchEntity(String title) {
        Map<String, String> jsonMap = new HashMap<>();
        jsonMap.put("searchString", title);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(jsonMap, headers);
    }


    public ExportResult checkForDuplicate(Item item, ExportType exportType) {
        int offset = 0;
        int pageSize = 100;
        boolean itemsLeft = true;

        String title =
            dSpaceServicesContainer.getItemService().getMetadataFirstValue(item, "dc", "title", null, Item.ANY);

        List<MetadataValue> identifierMetadataValues =
            dSpaceServicesContainer.getItemService().getMetadataByMetadataString(item, "dc.identifier.uri");
        PrimaryIdentifier primaryIdentifier =
            PrimaryIdentifierHelper.getPrimaryIdentifier(item, identifierMetadataValues);
        String doi = (primaryIdentifier != null && primaryIdentifier.isDoi() ? primaryIdentifier.getUrl() : null);

        if (exportType.getSearchResultClass() != null && title != null) {
            HttpEntity<Map<String, String>> searchEntity = createTitleSearchEntity(title);

            do {
                String webServiceUrl =
                    leuphanaPureWsEndpointBase + exportType.getPureEndpoint() + "/search?offset=" + offset + "&size=" +
                        pageSize;
                log.info("WebService Call: {}", webServiceUrl);

                ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntity =
                    duplicateCheckRestTemplate.postForEntity(webServiceUrl, searchEntity,
                        exportType.getSearchResultClass());
                PureWSResults<? extends PureWSResultItem> pureWSResults = responseEntity.getBody();
                if (pureWSResults == null) {
                    throw new IllegalStateException("No response body received from Pure WS!");
                }

                if (pureWSResults.getItems() == null || pureWSResults.getItems().size() < pageSize) {
                    itemsLeft = false;
                } else {
                    offset += pageSize;
                }

                boolean doublet = false;
                for (PureWSResultItem pureWSResultItem : pureWSResults.getItems()) {
                    DuplicateCheckResult duplicateCheck = concreteDuplicateCheck(pureWSResultItem, doi, title);
                    if (DuplicateCheckResult.DOI_DUPLICATE.equals(duplicateCheck)) {
                        log.info("Doublet detected for item {} and doi {}", item.getHandle(), doi);
                        doublet = true;
                    } else if (DuplicateCheckResult.TITLE_DUPLICATE.equals(duplicateCheck)) {
                        log.info("Doublet detected for item {} and title {}", item.getHandle(), title);
                        doublet = true;
                    }
                    if (doublet) {
                        ExportResult exportResult = new ExportResult();
                        exportResult.setUuid(pureWSResultItem.getUuid());
                        exportResult.setPortalUrl(pureWSResultItem.getPortalUrl());
                        return exportResult;
                    }
                }
            } while (itemsLeft);
        }
        return null;
    }

    public void init() throws ApiException {
        defaultOrganizationUUID = UUID.fromString(dSpaceServicesContainer.getConfigurationService().getProperty(
            "dspace-pure-bridge.export.defaultOrganizationUUID"));
        defaultAuthorUUID = UUID.fromString(dSpaceServicesContainer.getConfigurationService().getProperty(
            "dspace-pure-bridge.export.defaultAuthorUUID"));
        defaultAuthorFirstName = dSpaceServicesContainer.getConfigurationService().getProperty(
            "dspace-pure-bridge.export.defaultAuthorFirstName");
        defaultAuthorLastName = dSpaceServicesContainer.getConfigurationService().getProperty(
            "dspace-pure-bridge.export.defaultAuthorLastName");
        for (DSpaceLanguage dSpaceLanguage : DSpaceLanguage.values()) {
            languageDSpaceMap.put(dSpaceLanguage.getIso2Letter(), dSpaceLanguage);
        }
        ServerConfiguration serverConfiguration = new ServerConfiguration(
            leuphanaPureWsEndpointBase,
            "No description provided",
            new HashMap<>());

        apiClient = Configuration.getDefaultApiClient();
        apiClient.setApiKey(leuphanaPureWsApiKey);
        apiClient.setServers(List.of(serverConfiguration));
    }
}
