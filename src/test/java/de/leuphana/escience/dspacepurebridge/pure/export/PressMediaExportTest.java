package de.leuphana.escience.dspacepurebridge.pure.export;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.leuphana.escience.dspacepurebridge.Constants;
import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PressMediaCoverage;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultPressMediaItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResults;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.PressMediaApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRefList;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.InternalPressMediaPersonAssociation;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.MediaCoverage;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.PressMedia;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.Visibility;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpaceObjectMappings;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
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
public class PressMediaExportTest {
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
    private PressMediaApi pressMediaApi;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ExportStatus exportStatus = new ExportStatus("TEST");

    @Spy
    @InjectMocks
    private PressMediaExport classUnderTest;

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
        mapping.put(PressMediaExport.MAPPING_PROPERTIES_PREFIX + "." + PressMediaMappingType.TYPE + ".Sound",
                "/dk/atira/pure/pressmedia/pressmediatypes/pressmedia/interview");
        mapping.put(PressMediaExport.MAPPING_PROPERTIES_PREFIX + "." + PressMediaMappingType.CONTRIBUTOR_ROLE + ".Author",
                "/dk/atira/pure/pressmedia/roles/pressmedia/author");

        when(configurationService.getPropertyKeys(PressMediaExport.MAPPING_PROPERTIES_PREFIX)).thenReturn(
                new ArrayList<>(mapping.keySet()));
        for (String key : mapping.keySet()) {
            when(configurationService.getProperty(key)).thenReturn(mapping.get(key));
        }
        classUnderTest.setupMappingFromConfiguration();

        Assertions.assertEquals("/dk/atira/pure/pressmedia/pressmediatypes/pressmedia/interview",
                classUnderTest.mapping.get(PressMediaMappingType.TYPE).get("Sound"));
        Assertions.assertEquals("/dk/atira/pure/pressmedia/roles/pressmedia/author",
                classUnderTest.mapping.get(PressMediaMappingType.CONTRIBUTOR_ROLE).get("Author"));
    }

    @Test
    public void setupMappingWithInvalidMappingTypeFromConfiguration() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Map<String, String> mapping = new HashMap<>();
            mapping.put(PressMediaExport.MAPPING_PROPERTIES_PREFIX + ".UNKNOWN.MappingValueXY", "/xy/12");

            when(configurationService.getPropertyKeys(PressMediaExport.MAPPING_PROPERTIES_PREFIX)).thenReturn(
                    new ArrayList<>(mapping.keySet()));
            classUnderTest.setupMappingFromConfiguration();
        });
    }

    @Test
    public void createPressMedia() throws ApiException, SQLException {
        final String publicationType = "Sound";
        final String typeUrl = "/type/interview";
        final String roleUrl = "/role/author";
        final String titleGerman = "PRESSEMITTEILUNG DEU";
        final String descriptionGerman = "ABSTRACT DEU";
        final String publicationYear = "2022";
        final String firstName = "firstName";
        final String lastName = "lastName";
        final UUID defaultOrganizationUUID = UUID.randomUUID();
        final UUID defaultAuthorUUID = UUID.randomUUID();
        final UUID authorPureUUID = UUID.randomUUID();

        when(configurationService.getProperty("dspace-pure-bridge.export.defaultOrganizationUUID"))
                .thenReturn(defaultOrganizationUUID.toString());
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorUUID"))
                .thenReturn(defaultAuthorUUID.toString());
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorFirstName"))
                .thenReturn("default");
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorLastName"))
                .thenReturn("author");

        Map<PressMediaMappingType, Map<String, String>> mapping = classUnderTest.mapping;
        for (PressMediaMappingType mappingType : PressMediaMappingType.values()) {
            mapping.put(mappingType, new HashMap<>());
        }
        mapping.get(PressMediaMappingType.TYPE).put(publicationType, typeUrl);
        mapping.get(PressMediaMappingType.CONTRIBUTOR_ROLE).put(DSpaceToPure.PURE_AUTHOR_ROLE, roleUrl);

        MetadataValue titleMetadataValue = mock(MetadataValue.class);
        when(titleMetadataValue.getValue()).thenReturn(titleGerman);
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.title"))
                .thenReturn(List.of(titleMetadataValue));

        MetadataValue coverageDateMetadataValue = mock(MetadataValue.class);
        when(coverageDateMetadataValue.getValue()).thenReturn(publicationYear);
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.date.issued"))
                .thenReturn(List.of(coverageDateMetadataValue));

        MetadataValue descriptionMetadataValue = mock(MetadataValue.class);
        when(descriptionMetadataValue.getValue()).thenReturn(descriptionGerman);
        when(itemService.getMetadataByMetadataString(publicationItem, "DataCite.Description.Abstract"))
                .thenReturn(List.of(descriptionMetadataValue));

        when(itemService.getMetadataByMetadataString(publicationItem, "local.Affiliation"))
                .thenReturn(List.of());

        ClassificationRef type = new ClassificationRef();
        type.setUri(typeUrl);
        ClassificationRefList typesRefList = new ClassificationRefList();
        typesRefList.addClassificationsItem(type);
        when(pressMediaApi.pressmediaGetAllowedTypes()).thenReturn(typesRefList);

        ClassificationRef role = new ClassificationRef();
        role.setUri(roleUrl);
        ClassificationRefList roleRefList = new ClassificationRefList();
        roleRefList.addClassificationsItem(role);
        when(pressMediaApi.pressMediaGetAllowedMediaCoveragesPersonsRoles()).thenReturn(roleRefList);

        Item author = mock(Item.class);
        when(itemService.getMetadataFirstValue(author, "person", "givenName", null, Item.ANY)).thenReturn(firstName);
        when(itemService.getMetadataFirstValue(author, "person", "familyName", null, Item.ANY)).thenReturn(lastName);
        when(itemService.getMetadataFirstValue(author, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER,
                Item.ANY)).thenReturn(authorPureUUID.toString());
        doReturn(List.of(author)).when(classUnderTest).getItemAuthors(context, publicationItem, true);

        doReturn(pressMediaApi).when(classUnderTest).createPressMediaApiClient();
        doNothing().when(classUnderTest).setupMappingFromConfiguration();
        classUnderTest.init();

        ExportItem exportItem = classUnderTest.createExport(context, publicationItem, publicationType);
        PressMedia pressMedia = (PressMedia) exportItem.export();

        Assertions.assertEquals(titleGerman, pressMedia.getTitle().get(DSpaceLanguage.GERMAN.getLocale()));
        Assertions.assertEquals(typeUrl, pressMedia.getType().getUri());
        Assertions.assertNull(pressMedia.getWorkflow());
        Assertions.assertEquals(Visibility.KeyEnum.FREE, pressMedia.getVisibility().getKey());

        MediaCoverage mediaCoverage = pressMedia.getMediaCoverages().get(0);
        Assertions.assertEquals(titleGerman, mediaCoverage.getTitle().get(DSpaceLanguage.GERMAN.getLocale()));
        Assertions.assertEquals(MediaCoverage.CoverageTypeEnum.CONTRIBUTION, mediaCoverage.getCoverageType());
        Assertions.assertEquals(LocalDate.of(2022, 1, 1), mediaCoverage.getDate());
        Assertions.assertEquals(descriptionGerman, mediaCoverage.getDescription().get(DSpaceLanguage.GERMAN.getLocale()));

        InternalPressMediaPersonAssociation person =
                (InternalPressMediaPersonAssociation) mediaCoverage.getPersons().get(0);
        Assertions.assertEquals(authorPureUUID, person.getPerson().getUuid());
        Assertions.assertEquals(firstName, person.getName().getFirstName());
        Assertions.assertEquals(lastName, person.getName().getLastName());
        Assertions.assertEquals(roleUrl, person.getRole().getUri());
    }

    private PureWSResultPressMediaItem pressMediaResultItem(String title, String coverageDate, String coverageUrl) {
        PressMediaCoverage mediaCoverage = new PressMediaCoverage();
        mediaCoverage.setDate(coverageDate);
        mediaCoverage.setUrl(coverageUrl);
        PureWSResultPressMediaItem pressMediaItem = new PureWSResultPressMediaItem();
        pressMediaItem.setUuid(UUID.randomUUID());
        pressMediaItem.setTitle(Map.of("de_DE", title));
        pressMediaItem.setMediaCoverages(List.of(mediaCoverage));
        return pressMediaItem;
    }

    private void mockItemCoverageDate(String date) {
        MetadataValue coverageDateMetadataValue = mock(MetadataValue.class);
        when(coverageDateMetadataValue.getValue()).thenReturn(date);
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.date.issued"))
            .thenReturn(List.of(coverageDateMetadataValue));
    }

    @Test
    public void concreteDuplicateCheckFindsDoiInMediaCoverageUrl() {
        String doi = "https://doi.org/10.1234/xyz";
        PureWSResultPressMediaItem pressMediaItem = pressMediaResultItem("Anderer Titel", "2024-01-01", doi);

        Assertions.assertEquals(DuplicateCheckResult.DOI_DUPLICATE,
            classUnderTest.concreteDuplicateCheck(publicationItem, pressMediaItem, doi, "Der Titel"));
    }

    @Test
    public void concreteDuplicateCheckFindsTitleWithSameCoverageDate() {
        mockItemCoverageDate("2024-03-15");
        PureWSResultPressMediaItem pressMediaItem =
            pressMediaResultItem("Interview &amp; Gespräch", "2024-03-15", null);

        Assertions.assertEquals(DuplicateCheckResult.TITLE_DUPLICATE,
            classUnderTest.concreteDuplicateCheck(publicationItem, pressMediaItem, null, "interview  & Gespräch"));
    }

    @Test
    public void concreteDuplicateCheckIgnoresTitleWithDifferentCoverageDate() {
        mockItemCoverageDate("2024-03-15");
        PureWSResultPressMediaItem pressMediaItem = pressMediaResultItem("Interview", "2023-11-02", null);

        Assertions.assertEquals(DuplicateCheckResult.NO_DUPLICATE,
            classUnderTest.concreteDuplicateCheck(publicationItem, pressMediaItem, null, "Interview"));
    }

    @Test
    public void concreteDuplicateCheckFallsBackToTitleWithoutCoverageDate() {
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.date.issued")).thenReturn(List.of());
        PureWSResultPressMediaItem pressMediaItem = pressMediaResultItem("Interview", "2023-11-02", null);

        Assertions.assertEquals(DuplicateCheckResult.TITLE_DUPLICATE,
            classUnderTest.concreteDuplicateCheck(publicationItem, pressMediaItem, null, "Interview"));
    }

    @Test
    public void concreteDuplicateCheckWithDifferentTitle() {
        PureWSResultPressMediaItem pressMediaItem = pressMediaResultItem("Ein anderer Titel", "2024-03-15", null);

        Assertions.assertEquals(DuplicateCheckResult.NO_DUPLICATE,
            classUnderTest.concreteDuplicateCheck(publicationItem, pressMediaItem, null, "Der Titel"));
    }

    @Test
    public void checkForDuplicateSearchesPressMediaEndpoint() {
        String title = "Der Titel";
        mockItemCoverageDate("2024");
        PureWSResultPressMediaItem pressMediaItem = pressMediaResultItem(title, "2024-01-01", null);
        ExportType.PureWSPressMediaResults results = new ExportType.PureWSPressMediaResults();
        results.setItems(List.of(pressMediaItem));

        ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntity = mock(ResponseEntity.class);
        when(responseEntity.getBody()).thenAnswer(
            (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> results);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenAnswer(
            (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> responseEntity);
        when(itemService.getMetadataFirstValue(publicationItem, "dc", "title", null, Item.ANY)).thenReturn(title);
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.identifier.uri")).thenReturn(List.of());

        ExportResult exportResult = classUnderTest.checkForDuplicate(publicationItem, ExportType.PRESS_MEDIA);

        verify(restTemplate).postForEntity(endsWith("pressmedia/search?offset=0&size=100"), any(),
            any());
        Assertions.assertEquals(pressMediaItem.getUuid(), exportResult.getUuid());
    }
}
