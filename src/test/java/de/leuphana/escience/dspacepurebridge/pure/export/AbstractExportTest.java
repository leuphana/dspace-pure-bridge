package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.Constants;
import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpaceObjectMappings;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AbstractExportTest {
    @Mock
    private Context context;

    @Mock
    private Item publicationItem;

    @Mock
    private Item author;

    @Mock
    private ItemService itemService;

    @Spy
    private DSpaceObjectMappings dSpaceObjectMappings;

    @Mock
    private DSpaceServicesContainer dSpaceServicesContainer;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private RestTemplate duplicateCheckRestTemplate;


    @Spy
    private ExportStatus exportStatus = new ExportStatus("TEST");

    @Spy
    @InjectMocks
    private AbstractExport classUnderTest =
        new AbstractExport("", "", dSpaceServicesContainer, dSpaceObjectMappings, exportStatus,
            duplicateCheckRestTemplate) {
            @Override
            public void init() throws ApiException {

            }

            @Override
            public ExportItem createExport(Context context, Item item, String syncTypeValue)
                throws ApiException, SQLException {
                return null;
            }

            @Override
            public void setupMappingFromConfiguration() {

            }

            @Override
            public void setupClassifications() throws ApiException {

            }

            @Override
            public ExportResult export(ExportItem exportItem) throws ApiException {
                return null;
            }

            @Override
            public DuplicateCheckResult concreteDuplicateCheck(PureWSResultItem pureWSResultItem, String doi,
                                                               String title) {
                return null;
            }
        };

    @BeforeEach
    void setUp() {
        lenient().when(dSpaceServicesContainer.getItemService()).thenReturn(itemService);
    }

    @Test
    void getItemAuthorWithPureUUID() throws SQLException {
        String authorId = "TEST_AUTHOR_ID";
        MetadataValue authorRelation = mock(MetadataValue.class);
        when(authorRelation.getValue()).thenReturn(authorId);

        when(itemService.getMetadata(publicationItem, MetadataSchemaEnum.RELATION.getName(),
                                     "isAuthorOfPublication", null, Item.ANY)).thenReturn(List.of(authorRelation));
        when(itemService.findByIdOrLegacyId(context, authorId)).thenReturn(author);
        when(itemService.getMetadataFirstValue(author, Constants.SCHEME,
                                               Constants.ELEMENT,
                                               Constants.UUID_QUALIFIER,
                                               Item.ANY)).thenReturn("UUID");

        List<Item> result = classUnderTest.getItemAuthors(context, publicationItem, false);
        Assertions.assertEquals(author, result.get(0));
    }

    @Test
    void getItemAuthorWithoutPureUUID() throws SQLException {
        String authorId = "TEST_AUTHOR_ID";
        MetadataValue authorRelation = mock(MetadataValue.class);
        when(authorRelation.getValue()).thenReturn(authorId);

        when(itemService.getMetadata(publicationItem, MetadataSchemaEnum.RELATION.getName(),
            "isAuthorOfPublication", null, Item.ANY)).thenReturn(List.of(authorRelation));
        when(itemService.findByIdOrLegacyId(context, authorId)).thenReturn(author);
        when(itemService.getMetadataFirstValue(author, Constants.SCHEME,
            Constants.ELEMENT,
            Constants.UUID_QUALIFIER,
            Item.ANY)).thenReturn(null);

        List<Item> result = classUnderTest.getItemAuthors(context, publicationItem, false);
        Assertions.assertNull(result);
    }

    @Test
    void getItemAuthorWithoutPureUUIDAllowEmpty() throws SQLException {
        String authorId = "TEST_AUTHOR_ID";
        MetadataValue authorRelation = mock(MetadataValue.class);
        when(authorRelation.getValue()).thenReturn(authorId);

        when(itemService.getMetadata(publicationItem, MetadataSchemaEnum.RELATION.getName(),
            "isAuthorOfPublication", null, Item.ANY)).thenReturn(List.of(authorRelation));
        when(itemService.findByIdOrLegacyId(context, authorId)).thenReturn(author);
        when(itemService.getMetadataFirstValue(author, Constants.SCHEME,
            Constants.ELEMENT,
            Constants.UUID_QUALIFIER,
            Item.ANY)).thenReturn(null);

        List<Item> result = classUnderTest.getItemAuthors(context, publicationItem, true);
        Assertions.assertNotNull(result);
    }

    @Test
    void getItemAuthorWithoutPureUUIDAndOrcidCache() throws SQLException {
        String authorId = "TEST_AUTHOR_ID";
        MetadataValue authorRelation = mock(MetadataValue.class);
        when(authorRelation.getValue()).thenReturn(authorId);

        when(itemService.getMetadata(publicationItem, MetadataSchemaEnum.RELATION.getName(),
                                     "isAuthorOfPublication", null, Item.ANY)).thenReturn(List.of(authorRelation));
        when(itemService.findByIdOrLegacyId(context, authorId)).thenReturn(author);
        when(itemService.getMetadataFirstValue(author, Constants.SCHEME,
                                               Constants.ELEMENT,
                                               Constants.UUID_QUALIFIER,
                                               Item.ANY)).thenReturn(null);

        List<Item> result = classUnderTest.getItemAuthors(context, publicationItem, false);
        Assertions.assertNull(result);
    }

    @Test
    void createSearchEntityTest() {
        String title = "Title XY";
        HttpEntity<Map<String, String>> searchEntity = classUnderTest.createTitleSearchEntity(title);
        Assertions.assertNotNull(searchEntity.getBody());
        Assertions.assertEquals(title, searchEntity.getBody().get("searchString"));
    }
}
