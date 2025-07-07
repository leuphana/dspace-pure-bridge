/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import static de.leuphana.escience.export.identifiers.Identifiers.DOI_RESOLVER_HTTPS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.leuphana.escience.pure.Constants;
import de.leuphana.escience.pure.generated.ApiException;
import de.leuphana.escience.pure.generated.api.ResearchOutputApi;
import de.leuphana.escience.pure.generated.model.ClassificationRef;
import de.leuphana.escience.pure.generated.model.ClassificationRefList;
import de.leuphana.escience.pure.generated.model.DoiElectronicVersion;
import de.leuphana.escience.pure.generated.model.InternalContributorAssociation;
import de.leuphana.escience.pure.generated.model.ResearchOutput;
import de.leuphana.escience.pure.apiobjects.AccessType;
import de.leuphana.escience.pure.imports.DSpaceObjectMappings;
import de.leuphana.escience.pure.DSpaceServicesContainer;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResearchOutputExportTest {
    @Mock
    private Context context;

    @Mock
    private Item publicationItem;

    @Mock
    private ItemService itemService;

    @Mock
    private DSpaceServicesContainer dSpaceServicesContainer;

    @Spy
    private DSpaceObjectMappings dSpaceObjectMappings;

    @Mock
    private ResearchOutputApi researchOutputApi;

    @Mock
    private ConfigurationService configurationService;

    @Spy
    private ExportStatus exportStatus = new ExportStatus("TEST");

    @Spy
    @InjectMocks
    private ResearchOutputExport classUnderTest;

    @Before
    public void setUp() throws Exception {
        when(dSpaceServicesContainer.getConfigurationService()).thenReturn(configurationService);
        when(dSpaceServicesContainer.getItemService()).thenReturn(itemService);
    }

    @Test
    public void setupMappingFromConfiguration() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(ResearchOutputExport.mappingPropertiesPrefix + "." +
                        ResearchOutputMappingType.ACCESS_TYPE + "." + AccessType.EMBARGO,
                    "/dk/atira/pure/core/openaccesspermission/embargoed");
        mapping.put(
            ResearchOutputExport.mappingPropertiesPrefix + "." +
                ResearchOutputMappingType.LICENSE + ".Nutzung nach Urheberrecht",
            "/dk/atira/pure/core/document/licenses/unspecified");
        mapping.put(
            ResearchOutputExport.mappingPropertiesPrefix + "." +
                ResearchOutputMappingType.LICENSE + ".CC-BY-ND",
            "/dk/atira/pure/core/document/licenses/cc_by_nd");
        mapping.put(
            ResearchOutputExport.mappingPropertiesPrefix + "." +
                ResearchOutputMappingType.ROLE + ".Dissertation-Author",
            "/dk/atira/pure/researchoutput/roles/bookanthology/author");


        when(configurationService.getPropertyKeys(ResearchOutputExport.mappingPropertiesPrefix)).thenReturn(
            new ArrayList<>(mapping.keySet()));
        for (String key : mapping.keySet()) {
            when(configurationService.getProperty(key)).thenReturn(mapping.get(key));
        }
        classUnderTest.setupMappingFromConfiguration();

        assertNotEquals(0, classUnderTest.mapping.size());
        assertEquals("/dk/atira/pure/core/document/licenses/cc_by_nd",
                     classUnderTest.mapping.get(ResearchOutputMappingType.LICENSE).get("CC-BY-ND"));
        assertEquals("/dk/atira/pure/core/document/licenses/unspecified",
                     classUnderTest.mapping.get(ResearchOutputMappingType.LICENSE).get("Nutzung nach Urheberrecht"));
        assertEquals("/dk/atira/pure/core/openaccesspermission/embargoed",
                     classUnderTest.mapping.get(ResearchOutputMappingType.ACCESS_TYPE)
                                           .get(AccessType.EMBARGO.toString()));
        assertEquals("/dk/atira/pure/researchoutput/roles/bookanthology/author",
                     classUnderTest.mapping.get(ResearchOutputMappingType.ROLE).get("Dissertation-Author"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setupMappingWithInvalidMappingFromConfigurationType() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(
            ResearchOutputExport.mappingPropertiesPrefix + ".UNKNOWN.MappingValueXY",
            "/dk/atira/pure/core/document/xy/12");

        when(configurationService.getPropertyKeys(ResearchOutputExport.mappingPropertiesPrefix)).thenReturn(
            new ArrayList<>(mapping.keySet()));
        classUnderTest.setupMappingFromConfiguration();
    }

    @Test(expected = IllegalArgumentException.class)
    public void setupMappingFromConfigurationWithInvalidKey() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(
            ResearchOutputExport.mappingPropertiesPrefix + ".MappingValueXY",
            "/dk/atira/pure/core/document/xy/12");

        when(configurationService.getPropertyKeys(ResearchOutputExport.mappingPropertiesPrefix)).thenReturn(
            new ArrayList<>(mapping.keySet()));
        classUnderTest.setupMappingFromConfiguration();
    }

    @Test
    public void createResearchOutputDissertation() throws ApiException, SQLException {
        MetadataValue creationContextMetadataValue = mock(MetadataValue.class);
        MetadataValue publicationYearMetadataValue = mock(MetadataValue.class);
        MetadataValue titleDeuMetadataValue = mock(MetadataValue.class);
        MetadataValue titleEngMetadataValue = mock(MetadataValue.class);
        MetadataValue subTitleDeuMetadataValue = mock(MetadataValue.class);
        MetadataValue subTitleEngMetadataValue = mock(MetadataValue.class);
        MetadataValue abstractDeuMetadataValue = mock(MetadataValue.class);
        MetadataValue abstractEngMetadataValue = mock(MetadataValue.class);
        MetadataValue identifierMetadataValue = mock(MetadataValue.class);
        MetadataValue affiliationMetadataValue = mock(MetadataValue.class);
        final String publicationLanguage = DSpaceLanguage.GERMAN.getIso3Letter();
        final String publicationType = "Dissertation";
        final String researchValue = "Research";
        final String researchValueUrl = "/category/research";
        final String dissertationUrl = "/type/dissertation";
        final String publishedUrl = "/published/published";
        final String germanUrl = "/language/german";
        final String englishUrl = "/language/english";
        final String openAccessUrl = "/accessType/openAccess";
        final String versionTypeUrl = "/versionType/other";
        final String testLicense = "TEST LICENSE";
        final String licenseUrl = "/license/open";
        final String authorUrl = "/role/author";
        final String publicationYear = "2022";
        final String titleGerman = "TITEL DEU";
        final String titleEnglish = "TITLE ENG";
        final String subTitleGerman = "UNTERTITEL DEU";
        final String subTitleEnglish = "SUBTITLE ENG";
        final String abstractGerman = "ABSTRAKT DEU";
        final String abstractEnglish = "ABSTRACT ENG";
        final String doiValue = DOI_RESOLVER_HTTPS + "/DOITEST/XY";
        final String orgUnit = "ORGUNIT A";
        final String firstName = "firstName";
        final String lastName = "lastName";
        final UUID defaultOrganizationUUID = UUID.randomUUID();
        final UUID defaultAuthorUUID = UUID.randomUUID();
        final String defaultAuthorFirstName = "default";
        final String defaultAuthorLastName = "author";

        when(configurationService.getProperty("leuphana.pure.sync.defaultOrganizationUUID"))
            .thenReturn(defaultOrganizationUUID.toString());
        when(configurationService.getProperty("leuphana.pure.sync.defaultAuthorUUID")).thenReturn(
            defaultAuthorUUID.toString());
        when(configurationService.getProperty("leuphana.pure.sync.defaultAuthorFirstName")).thenReturn(defaultAuthorFirstName);
        when(configurationService.getProperty("leuphana.pure.sync.defaultAuthorLastName")).thenReturn(defaultAuthorLastName);

        Map<ResearchOutputMappingType, Map<String, String>> mapping = classUnderTest.mapping;
        for (ResearchOutputMappingType mappingType : ResearchOutputMappingType.values()) {
            mapping.put(mappingType, new HashMap<>());
        }
        mapping.get(ResearchOutputMappingType.CREATION_CONTEXT).put(researchValue, researchValueUrl);
        mapping.get(ResearchOutputMappingType.TYPE).put(publicationType, dissertationUrl);
        mapping.get(ResearchOutputMappingType.PUBLISHED).put(DSpaceToPure.DEFAULT_METADATA_VALUE, publishedUrl);
        mapping.get(ResearchOutputMappingType.LANGUAGE).put(publicationLanguage, germanUrl);
        mapping.get(ResearchOutputMappingType.LANGUAGE).put(DSpaceLanguage.ENGLISH.getIso3Letter(), englishUrl);
        mapping.get(ResearchOutputMappingType.ACCESS_TYPE).put(String.valueOf(AccessType.OPEN_ACCESS), openAccessUrl);
        mapping.get(ResearchOutputMappingType.ELECTRONIC_VERSION_TYPE)
               .put(DSpaceToPure.DEFAULT_METADATA_VALUE, versionTypeUrl);
        mapping.get(ResearchOutputMappingType.LICENSE).put(testLicense, licenseUrl);
        mapping.get(ResearchOutputMappingType.ROLE)
               .put(publicationType + "-" + DSpaceToPure.PURE_AUTHOR_ROLE, authorUrl);

        when(creationContextMetadataValue.getValue()).thenReturn(researchValue);
        when(itemService.getMetadataByMetadataString(publicationItem, "local.CreationContext")).thenReturn(
            Collections.singletonList(creationContextMetadataValue));
        when(itemService.getMetadataFirstValue(
            publicationItem, "DataCite", "Language", null, Item.ANY))
            .thenReturn(publicationLanguage);
        when(publicationYearMetadataValue.getValue()).thenReturn(publicationYear);
        when(itemService.getMetadataByMetadataString(publicationItem, "DataCite.PublicationYear")).thenReturn(
            Collections.singletonList(publicationYearMetadataValue));
        when(titleDeuMetadataValue.getValue()).thenReturn(titleGerman);
        when(titleEngMetadataValue.getValue()).thenReturn(titleEnglish);
        when(titleEngMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.ENGLISH.getIso2Letter());
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.title")).thenReturn(
            List.of(titleDeuMetadataValue, titleEngMetadataValue));
        when(subTitleDeuMetadataValue.getValue()).thenReturn(subTitleGerman);
        when(subTitleDeuMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.GERMAN.getIso2Letter());
        when(subTitleEngMetadataValue.getValue()).thenReturn(subTitleEnglish);
        when(subTitleEngMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.ENGLISH.getIso2Letter());
        when(itemService.getMetadataByMetadataString(publicationItem, "DataCite.Title.Subtitle")).thenReturn(
            List.of(subTitleDeuMetadataValue, subTitleEngMetadataValue));
        when(abstractDeuMetadataValue.getValue()).thenReturn(abstractGerman);
        when(abstractDeuMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.GERMAN.getIso2Letter());
        when(abstractEngMetadataValue.getValue()).thenReturn(abstractEnglish);
        when(abstractEngMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.ENGLISH.getIso2Letter());
        when(itemService.getMetadataByMetadataString(
            publicationItem, "DataCite.Description.Abstract")).thenReturn(
            List.of(abstractDeuMetadataValue, abstractEngMetadataValue));
        when(identifierMetadataValue.getValue()).thenReturn(doiValue);
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.identifier.uri")).thenReturn(
            Collections.singletonList(identifierMetadataValue));
        when(affiliationMetadataValue.getValue()).thenReturn(orgUnit);
        when(itemService.getMetadataByMetadataString(publicationItem, "local.Affiliation")).thenReturn(
            Collections.singletonList(affiliationMetadataValue));


        ClassificationRef research = new ClassificationRef();
        research.setUri(researchValueUrl);
        ClassificationRefList categoriesRefList = new ClassificationRefList();
        categoriesRefList.addClassificationsItem(research);
        when(researchOutputApi.researchoutputGetAllowedCategories()).thenReturn(categoriesRefList);

        ClassificationRef type = new ClassificationRef();
        type.setUri(dissertationUrl);
        ClassificationRefList typesRefList = new ClassificationRefList();
        typesRefList.addClassificationsItem(type);
        when(researchOutputApi.researchOutputGetAllowedTypes()).thenReturn(typesRefList);

        ClassificationRef published = new ClassificationRef();
        published.setUri(publishedUrl);
        ClassificationRefList publishedRefList = new ClassificationRefList();
        publishedRefList.addClassificationsItem(published);
        when(researchOutputApi.researchoutputGetAllowedPublicationStatuses()).thenReturn(publishedRefList);

        ClassificationRef german = new ClassificationRef();
        german.setUri(germanUrl);
        ClassificationRef english = new ClassificationRef();
        english.setUri(englishUrl);
        ClassificationRefList languagesRefList = new ClassificationRefList();
        languagesRefList.addClassificationsItem(german);
        languagesRefList.addClassificationsItem(english);
        when(researchOutputApi.researchoutputGetAllowedLanguages()).thenReturn(languagesRefList);

        ClassificationRef accessType = new ClassificationRef();
        accessType.setUri(openAccessUrl);
        ClassificationRefList accessTypeRefList = new ClassificationRefList();
        accessTypeRefList.addClassificationsItem(accessType);
        when(researchOutputApi.researchoutputGetAllowedElectronicVersionAccessTypes()).thenReturn(accessTypeRefList);

        ClassificationRef versionType = new ClassificationRef();
        versionType.setUri(versionTypeUrl);
        ClassificationRefList versionTypeRefList = new ClassificationRefList();
        versionTypeRefList.addClassificationsItem(versionType);
        when(researchOutputApi.researchoutputGetAllowedElectronicVersionVersionTypes()).thenReturn(versionTypeRefList);

        ClassificationRef licenseType = new ClassificationRef();
        licenseType.setUri(licenseUrl);
        ClassificationRefList licensesRefList = new ClassificationRefList();
        licensesRefList.addClassificationsItem(licenseType);
        when(researchOutputApi.researchoutputGetAllowedElectronicVersionLicenseTypes()).thenReturn(licensesRefList);

        ClassificationRef authorRole = new ClassificationRef();
        authorRole.setUri(authorUrl);
        ClassificationRefList authorRoleRefList = new ClassificationRefList();
        authorRoleRefList.addClassificationsItem(authorRole);
        when(researchOutputApi.researchoutputGetAllowedBookAnthologyContributorRoles()).thenReturn(authorRoleRefList);

        ElectronicVersionAccessDetail electronicVersionAccessDetail = new ElectronicVersionAccessDetail();
        electronicVersionAccessDetail.setAccessType(AccessType.OPEN_ACCESS);
        electronicVersionAccessDetail.setLicense(testLicense);
        doReturn(electronicVersionAccessDetail).when(classUnderTest).getElectronicVersionAccessDetail(context,
                                                                                                      publicationItem);

        UUID authorPureUUID = UUID.randomUUID();
        Item author = mock(Item.class);
        when(itemService.getMetadataFirstValue(
            author, "person", "givenName", null, Item.ANY))
            .thenReturn(firstName);
        when(itemService.getMetadataFirstValue(
            author, "person", "familyName", null, Item.ANY))
            .thenReturn(lastName);
        when(itemService.getMetadataFirstValue(
            author, Constants.SCHEME,
            Constants.ELEMENT,
            Constants.UUID_QUALIFIER,
            Item.ANY))
            .thenReturn(authorPureUUID.toString());


        doReturn(List.of(author)).when(classUnderTest).getItemAuthors(context, publicationItem, false);

        doReturn(researchOutputApi).when(classUnderTest).createResearchOutputApiClient();
        doNothing().when(classUnderTest).setupMappingFromConfiguration();
        classUnderTest.init();
        ExportItem exportItem = classUnderTest.createExport(context, publicationItem, publicationType);
        ResearchOutput researchOutput = (ResearchOutput) exportItem.export();

        assertEquals(titleGerman, researchOutput.getTitle().getValue());
        assertEquals(titleEnglish, researchOutput.getTranslatedTitle().get(DSpaceLanguage.ENGLISH.getLocale()));
        assertEquals(doiValue,
                     ((DoiElectronicVersion) researchOutput.getElectronicVersions().get(0)).getDoi());
        assertEquals(openAccessUrl,
                     researchOutput.getElectronicVersions().get(0).getAccessType().getUri());
        assertEquals(licenseUrl,
                     researchOutput.getElectronicVersions().get(0).getLicenseType().getUri());
        assertEquals(subTitleGerman, researchOutput.getSubTitle().getValue());
        assertEquals(subTitleEnglish, researchOutput.getTranslatedSubTitle().get(DSpaceLanguage.ENGLISH.getLocale()));
        assertEquals(abstractGerman, researchOutput.getAbstract().get(DSpaceLanguage.GERMAN.getLocale()));
        assertEquals(abstractEnglish, researchOutput.getAbstract().get(DSpaceLanguage.ENGLISH.getLocale()));
        assertEquals(publicationYear,
                     String.valueOf(researchOutput.getPublicationStatuses().get(0).getPublicationDate().getYear()));
        assertEquals(researchValueUrl, researchOutput.getCategory().getUri());
        assertEquals(dissertationUrl, researchOutput.getType().getUri());
        assertEquals(authorPureUUID,
                     ((InternalContributorAssociation) researchOutput.getContributors().get(0)).getPerson().getUuid());
        assertEquals(firstName,
                     ((InternalContributorAssociation) researchOutput.getContributors().get(0)).getName()
                                                                                               .getFirstName());
        assertEquals(lastName,
                     ((InternalContributorAssociation) researchOutput.getContributors().get(0)).getName()
                                                                                               .getLastName());
        assertEquals(authorUrl,
                     ((InternalContributorAssociation) researchOutput.getContributors().get(0)).getRole().getUri());
        assertEquals(defaultOrganizationUUID, researchOutput.getOrganizations().get(0).getUuid());
    }
}