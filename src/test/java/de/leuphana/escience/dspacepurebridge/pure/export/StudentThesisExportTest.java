/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.dspacepurebridge.pure.export;

import static de.leuphana.escience.dspacepurebridge.identifiers.Identifiers.DOI_RESOLVER_HTTPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.leuphana.escience.dspacepurebridge.pure.Constants;
import de.leuphana.escience.dspacepurebridge.pure.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.Link;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultStudentThesisItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResults;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.Value;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.StudentThesisApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRefList;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.InternalContributorAssociation;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.StudentThesis;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpaceObjectMappings;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class StudentThesisExportTest {
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
    private StudentThesisApi researchOutputApi;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ExportStatus exportStatus = new ExportStatus("TEST");

    @Spy
    @InjectMocks
    private StudentThesisExport classUnderTest;

    @BeforeEach
    public void setUp() {
        lenient().when(dSpaceServicesContainer.getConfigurationService()).thenReturn(configurationService);
        lenient().when(dSpaceServicesContainer.getItemService()).thenReturn(itemService);

        lenient().doAnswer(invocation -> invocation.getArgument(1)).when(configurationService)
            .getProperty(anyString(), anyString());
    }

    @Test
    public void setupMappingFromConfiguration() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(StudentThesisExport.MAPPING_PROPERTIES_PREFIX + "." + StudentThesisMappingType.CONTRIBUTOR_ROLE +
                ".Dissertation-Author",
            "/dk/atira/pure/studentthesis/roles/studentthesis/author");

        when(configurationService.getPropertyKeys(StudentThesisExport.MAPPING_PROPERTIES_PREFIX)).thenReturn(
            new ArrayList<>(mapping.keySet()));
        for (String key : mapping.keySet()) {
            when(configurationService.getProperty(key)).thenReturn(mapping.get(key));
        }
        classUnderTest.setupMappingFromConfiguration();
        assertEquals("/dk/atira/pure/studentthesis/roles/studentthesis/author", classUnderTest.mapping.get(StudentThesisMappingType.CONTRIBUTOR_ROLE).get("Dissertation-Author"));
    }

    @Test()
    public void setupMappingWithInvalidMappingFromConfigurationType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {

            Map<String, String> mapping = new HashMap<>();
            mapping.put(
                StudentThesisExport.MAPPING_PROPERTIES_PREFIX + ".UNKNOWN.MappingValueXY",
                "/dk/atira/pure/core/document/xy/12");

            when(configurationService.getPropertyKeys(StudentThesisExport.MAPPING_PROPERTIES_PREFIX)).thenReturn(
                new ArrayList<>(mapping.keySet()));
            classUnderTest.setupMappingFromConfiguration();
        });
    }

    @Test()
    public void setupMappingFromConfigurationWithInvalidKey() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Map<String, String> mapping = new HashMap<>();
            mapping.put(
                StudentThesisExport.MAPPING_PROPERTIES_PREFIX + ".MappingValueXY",
                "/dk/atira/pure/core/document/xy/12");

            when(configurationService.getPropertyKeys(StudentThesisExport.MAPPING_PROPERTIES_PREFIX)).thenReturn(
                new ArrayList<>(mapping.keySet()));
            classUnderTest.setupMappingFromConfiguration();
        });
    }

    @Test
    public void createStudentThesisDissertation() throws ApiException, SQLException {
        MetadataValue dateAcceptedMetadataValue = mock(MetadataValue.class);
        MetadataValue publicationLanguageMetadataValue = mock(MetadataValue.class);
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
        final String dissertationUrl = "/type/dissertation";
        final String germanUrl = "/language/german";
        final String englishUrl = "/language/english";
        final String authorUrl = "/role/author";
        final String reviewerUrl = "/role/reviewer";
        final String supervisorUrl = "/role/supervisor";
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
        final String dateAccepted = "2024";

        when(configurationService.getProperty("dspace-pure-bridge.export.defaultOrganizationUUID")).thenReturn(
            defaultOrganizationUUID.toString());
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorUUID")).thenReturn(
            defaultAuthorUUID.toString());
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorFirstName")).thenReturn(
            defaultAuthorFirstName);
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorLastName")).thenReturn(
            defaultAuthorLastName);

        Map<StudentThesisMappingType, Map<String, String>> mapping = classUnderTest.mapping;
        for (StudentThesisMappingType mappingType : StudentThesisMappingType.values()) {
            mapping.put(mappingType, new HashMap<>());
        }
        mapping.get(StudentThesisMappingType.TYPE).put(publicationType, dissertationUrl);
        mapping.get(StudentThesisMappingType.LANGUAGE).put(publicationLanguage, germanUrl);
        mapping.get(StudentThesisMappingType.LANGUAGE).put(DSpaceLanguage.ENGLISH.getIso3Letter(), englishUrl);
        mapping.get(StudentThesisMappingType.CONTRIBUTOR_ROLE)
            .put(DSpaceToPure.PURE_AUTHOR_ROLE,
                authorUrl);
        mapping.get(StudentThesisMappingType.SUPERVISOR_ROLE).put(DSpaceToPure.PURE_SUPERVISOR_ROLE, supervisorUrl);
        mapping.get(StudentThesisMappingType.SUPERVISOR_ROLE).put(DSpaceToPure.PURE_REVIEWER_ROLE, reviewerUrl);

        when(publicationLanguageMetadataValue.getValue()).thenReturn(publicationLanguage);
        when(itemService.getMetadataByMetadataString(publicationItem, "DataCite.Language")).thenReturn(
            List.of(publicationLanguageMetadataValue));

        when(dateAcceptedMetadataValue.getValue()).thenReturn(dateAccepted);
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.date.accepted")).thenReturn(
            List.of(dateAcceptedMetadataValue));

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
        when(itemService.getMetadataByMetadataString(publicationItem, "DataCite.Description.Abstract")).thenReturn(
            List.of(abstractDeuMetadataValue, abstractEngMetadataValue));
        when(identifierMetadataValue.getValue()).thenReturn(doiValue);
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.identifier.uri")).thenReturn(
            Collections.singletonList(identifierMetadataValue));
        when(affiliationMetadataValue.getValue()).thenReturn(orgUnit);
        when(itemService.getMetadataByMetadataString(publicationItem, "local.Affiliation")).thenReturn(
            Collections.singletonList(affiliationMetadataValue));

        ClassificationRef type = new ClassificationRef();
        type.setUri(dissertationUrl);
        ClassificationRefList typesRefList = new ClassificationRefList();
        typesRefList.addClassificationsItem(type);
        when(researchOutputApi.studentThesisGetAllowedTypes()).thenReturn(typesRefList);

        ClassificationRef german = new ClassificationRef();
        german.setUri(germanUrl);
        ClassificationRef english = new ClassificationRef();
        english.setUri(englishUrl);
        ClassificationRefList languagesRefList = new ClassificationRefList();
        languagesRefList.addClassificationsItem(german);
        languagesRefList.addClassificationsItem(english);
        when(researchOutputApi.studentthesisGetAllowedLanguages()).thenReturn(languagesRefList);

        ClassificationRef authorRole = new ClassificationRef();
        authorRole.setUri(authorUrl);
        ClassificationRefList authorRoleRefList = new ClassificationRefList();
        authorRoleRefList.addClassificationsItem(authorRole);
        when(researchOutputApi.studentthesisGetAllowedContributorRoles()).thenReturn(authorRoleRefList);

        ClassificationRef supervisorRole = new ClassificationRef();
        supervisorRole.setUri(supervisorUrl);
        ClassificationRef reviewerRole = new ClassificationRef();
        reviewerRole.setUri(reviewerUrl);
        ClassificationRefList supervisorRoleRefList = new ClassificationRefList();
        supervisorRoleRefList.addClassificationsItem(supervisorRole);
        supervisorRoleRefList.addClassificationsItem(reviewerRole);
        when(researchOutputApi.studentthesisGetAllowedSupervisorRoles()).thenReturn(supervisorRoleRefList);

        UUID authorPureUUID = UUID.randomUUID();
        Item author = mock(Item.class);
        when(itemService.getMetadataFirstValue(author, "person", "givenName", null, Item.ANY)).thenReturn(firstName);
        when(itemService.getMetadataFirstValue(author, "person", "familyName", null, Item.ANY)).thenReturn(lastName);
        when(itemService.getMetadataFirstValue(author, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER,
            Item.ANY)).thenReturn(authorPureUUID.toString());

        doReturn(List.of(author)).when(classUnderTest).getItemAuthors(context, publicationItem, false);

        doReturn(researchOutputApi).when(classUnderTest).createStudentThesisApiClient();
        doNothing().when(classUnderTest).setupMappingFromConfiguration();
        classUnderTest.init();
        ExportItem exportItem = classUnderTest.createExport(context, publicationItem, publicationType);
        StudentThesis studentThesis = (StudentThesis) exportItem.export();

        assertEquals(titleGerman, studentThesis.getTitle().getValue());
        assertEquals(titleEnglish,
            studentThesis.getTranslatedTitle().get(DSpaceLanguage.ENGLISH.getLocale()));
        assertEquals(subTitleGerman, studentThesis.getSubTitle().getValue());
        assertEquals(subTitleEnglish,
            studentThesis.getTranslatedSubTitle().get(DSpaceLanguage.ENGLISH.getLocale()));
        assertEquals(abstractGerman, studentThesis.getAbstract().get(DSpaceLanguage.GERMAN.getLocale()));
        assertEquals(abstractEnglish, studentThesis.getAbstract().get(DSpaceLanguage.ENGLISH.getLocale()));
        assertEquals(dissertationUrl, studentThesis.getType().getUri());
        assertEquals(authorPureUUID,
            ((InternalContributorAssociation) studentThesis.getContributors().get(0)).getPerson().getUuid());
        assertEquals(firstName,
            ((InternalContributorAssociation) studentThesis.getContributors().get(0)).getName()
                .getFirstName());
        assertEquals(lastName,
            ((InternalContributorAssociation) studentThesis.getContributors().get(0)).getName().getLastName());
        assertEquals(authorUrl,
            ((InternalContributorAssociation) studentThesis.getContributors().get(0)).getRole().getUri());
        assertEquals(dateAccepted, String.valueOf(studentThesis.getAwardDate().getYear()));
    }


    @Test
    public void checkForDoubletWithNoTitle() {
        Item item = mock(Item.class);

        ExportResult exportResult = classUnderTest.checkForDuplicate(item, ExportType.STUDENT_THESIS);
        Assertions.assertNull(exportResult);
        verify(classUnderTest, never()).createTitleSearchEntity(any());
    }

    @Test
    public void checkForDoubletWithDoubletFoundByDoi() {
        String title = "Der Titel";
        String doi = "https://doi.org/XYZ-DOI";
        UUID pureUUID = UUID.randomUUID();

        Item item = mock(Item.class);
        ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntity = mock(ResponseEntity.class);
        ExportType.PureWSStudentThesisResults results = mock(ExportType.PureWSStudentThesisResults.class);

        Link link = mock(Link.class);
        when(link.getUrl()).thenReturn(doi);
        PureWSResultStudentThesisItem pureWSResultStudentThesisItem = mock(PureWSResultStudentThesisItem.class);
        when(pureWSResultStudentThesisItem.getUuid()).thenReturn(pureUUID);
        when(pureWSResultStudentThesisItem.getLinks()).thenReturn(List.of(link));
        when(results.getItems()).thenReturn(List.of(pureWSResultStudentThesisItem));

        MetadataValue doiMetadataValue = mock(MetadataValue.class);
        when(doiMetadataValue.getValue()).thenReturn(doi);
        when(itemService.getMetadataByMetadataString(item, "dc.identifier.uri")).thenReturn(List.of(doiMetadataValue));
        when(itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY)).thenReturn(title);

        when(responseEntity.getBody()).thenAnswer(
            (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> results);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenAnswer(
            (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> responseEntity);

        ExportResult exportResult = classUnderTest.checkForDuplicate(item, ExportType.STUDENT_THESIS);

        verify(classUnderTest).createTitleSearchEntity(title);
        assertEquals(pureUUID, exportResult.getUuid());
    }

    @Test
    public void checkForDoubletWithDoubletFoundByTitle() {
        String title = "Der Titel";
        UUID pureUUID = UUID.randomUUID();

        Item item = mock(Item.class);
        ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntity = mock(ResponseEntity.class);
        ExportType.PureWSStudentThesisResults results = mock(ExportType.PureWSStudentThesisResults.class);

        Value titleValue = mock(Value.class);
        when(titleValue.getValue()).thenReturn(title);
        PureWSResultStudentThesisItem pureWSResultStudentThesisItem = mock(PureWSResultStudentThesisItem.class);
        when(pureWSResultStudentThesisItem.getTitle()).thenReturn(titleValue);
        when(pureWSResultStudentThesisItem.getUuid()).thenReturn(pureUUID);
        when(results.getItems()).thenReturn(List.of(pureWSResultStudentThesisItem));

        when(itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY)).thenReturn(title);

        when(responseEntity.getBody()).thenAnswer(
            (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> results);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenAnswer(
            (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> responseEntity);

        ExportResult exportResult = classUnderTest.checkForDuplicate(item, ExportType.STUDENT_THESIS);

        verify(classUnderTest).createTitleSearchEntity(title);
        assertEquals(pureUUID, exportResult.getUuid());
    }

    @Test
    public void checkForDoubletWithNoDoubletFound() {
        String title = "Der Titel";
        String pureTitle = "Der Titel in Pure";

        Item item = mock(Item.class);
        ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntity = mock(ResponseEntity.class);
        ExportType.PureWSStudentThesisResults results = mock(ExportType.PureWSStudentThesisResults.class);

        Value titleValue = mock(Value.class);
        when(titleValue.getValue()).thenReturn(pureTitle);
        PureWSResultStudentThesisItem pureWSResultStudentThesisItem = mock(PureWSResultStudentThesisItem.class);
        when(pureWSResultStudentThesisItem.getTitle()).thenReturn(titleValue);
        when(results.getItems()).thenReturn(List.of(pureWSResultStudentThesisItem));

        when(itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY)).thenReturn(title);

        when(responseEntity.getBody()).thenAnswer(
            (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> results);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenAnswer(
            (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> responseEntity);

        ExportResult exportResult = classUnderTest.checkForDuplicate(item, ExportType.STUDENT_THESIS);

        verify(classUnderTest).createTitleSearchEntity(title);
        Assertions.assertNull(exportResult);
    }

    @Test
    public void checkForDoubletWithNoPureWSResultItemResponseExpectIllegalStateException() {
        Assert.assertThrows(IllegalStateException.class, () -> {
            String title = "Der Titel";

            Item item = mock(Item.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntity =
                mock(ResponseEntity.class);

            when(itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY)).thenReturn(title);

            when(responseEntity.getBody()).thenReturn(null);
            when(restTemplate.postForEntity(anyString(), any(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> responseEntity);

            classUnderTest.checkForDuplicate(item, ExportType.STUDENT_THESIS);

            verify(classUnderTest).createTitleSearchEntity(title);
        });
    }
}