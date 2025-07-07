/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.leuphana.escience.pure.Constants;
import de.leuphana.escience.pure.generated.ApiClient;
import de.leuphana.escience.pure.generated.ApiException;
import de.leuphana.escience.pure.generated.Configuration;
import de.leuphana.escience.pure.generated.ServerConfiguration;
import de.leuphana.escience.pure.generated.model.AbstractContributorAssociation;
import de.leuphana.escience.pure.generated.model.ClassificationRef;
import de.leuphana.escience.pure.generated.model.FormattedString;
import de.leuphana.escience.pure.generated.model.InternalContributorAssociation;
import de.leuphana.escience.pure.generated.model.Name;
import de.leuphana.escience.pure.generated.model.Person;
import de.leuphana.escience.pure.generated.model.ResearchOutput;
import de.leuphana.escience.pure.generated.model.StudentThesis;
import de.leuphana.escience.export.identifiers.PrimaryIdentifier;
import de.leuphana.escience.export.identifiers.PrimaryIdentifierHelper;
import de.leuphana.escience.pure.apiobjects.AccessType;
import de.leuphana.escience.pure.imports.DSpaceObjectMappings;
import de.leuphana.escience.pure.DSpaceServicesContainer;
import de.leuphana.escience.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.pure.apiobjects.PureWSResults;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractExport {
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
    static final String generalMappingPropertiesPrefix = "leuphana.pure.sync.mapping";
    static final String authorRelation = "isAuthorOfPublication";
    static final String advisorRelation = "isAdvisorOfPublication";
    static final String refereeRelation = "isRefereeOfPublication";
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
                        String personOrcid = dSpaceServicesContainer
                            .getItemService()
                            .getMetadataFirstValue(person,
                                "person",
                                "identifier",
                                "orcid"
                                , Item.ANY);
                        if (personOrcid != null && dSpaceObjectMappings.getOrcidToPureMap().containsKey(personOrcid)) {
                            itemPersons.add(dSpaceObjectMappings.getOrcidToPureMap().get(personOrcid));
                        } else {
                            personsWithoutPureId.add(person);
                        }
                    }
                }
            }
        }
        if (!allowEmptyPersonList && authorRelation.equals(personToItemRelation)) {
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
        return getItemPersons(context, publicationItem, authorRelation, allowEmptyAuthorList);
    }

    List<Item> getItemAdvisors(Context context, org.dspace.content.Item publicationItem) throws SQLException {
        return getItemPersons(context, publicationItem, advisorRelation, false);
    }

    List<Item> getItemReferees(Context context, org.dspace.content.Item publicationItem) throws SQLException {
        return getItemPersons(context, publicationItem, refereeRelation, false);
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
        if (exportObject instanceof StudentThesis) {
            ((StudentThesis) exportObject).setAbstract(abstracts);
        }
        if (exportObject instanceof ResearchOutput) {
            ((ResearchOutput) exportObject).setAbstract(abstracts);
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
                    String license = dSpaceServicesContainer
                        .getBitstreamService()
                        .getMetadataFirstValue(bitstream,
                            "local",
                            "BitstreamLicense",
                            null,
                            Item.ANY);
                    electronicVersionAccessDetail.setAccessType(accessType);
                    electronicVersionAccessDetail.setLicense(license);
                } else {
                    for (Bitstream b : bnd.getBitstreams()) {
                        AccessType accessType = getAccessRightsValueForPolicies(context, dSpaceServicesContainer
                            .getAuthorizeService()
                            .getPoliciesActionFilter(
                                context, b,
                                org.dspace.core.Constants.READ));
                        String license = dSpaceServicesContainer
                            .getBitstreamService()
                            .getMetadataFirstValue(b,
                                "local",
                                "BitstreamLicense",
                                null,
                                Item.ANY);
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
            "leuphana.pure.sync.defaultOrganizationUUID"));
        defaultAuthorUUID = UUID.fromString(dSpaceServicesContainer.getConfigurationService().getProperty(
            "leuphana.pure.sync.defaultAuthorUUID"));
        defaultAuthorFirstName = dSpaceServicesContainer.getConfigurationService().getProperty(
            "leuphana.pure.sync.defaultAuthorFirstName");
        defaultAuthorLastName = dSpaceServicesContainer.getConfigurationService().getProperty(
            "leuphana.pure.sync.defaultAuthorLastName");
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

    protected abstract void setupMappingFromConfiguration();

    protected abstract void setupClassifications() throws ApiException;

    public abstract ExportItem createExport(Context context, Item item, String syncTypeValue) throws
        ApiException,
        SQLException;

    public abstract ExportResult export(ExportItem exportItem) throws ApiException;

    protected abstract DuplicateCheckResult concreteDuplicateCheck(PureWSResultItem pureWSResultItem, String doi,
                                                                   String title);
}
