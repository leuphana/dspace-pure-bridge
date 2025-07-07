/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.leuphana.escience.pure.Constants;
import de.leuphana.escience.pure.generated.ApiException;
import de.leuphana.escience.pure.apiobjects.AccessType;
import de.leuphana.escience.pure.imports.DSpaceObjectMappings;
import de.leuphana.escience.pure.DSpaceServicesContainer;
import de.leuphana.escience.pure.apiobjects.PureWSResultItem;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class AbstractExportTest {
    @Mock
    private Context context;

    @Mock
    private Item publicationItem;

    @Mock
    private Item author;

    @Mock
    private Item authorFromOrcidCache;

    @Mock
    private ItemService itemService;

    @Mock
    private ResourcePolicyService resourcePolicyService;

    @Mock
    private BitstreamService bitstreamService;

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
            protected void setupMappingFromConfiguration() {

            }

            @Override
            protected void setupClassifications() throws ApiException {

            }

            @Override
            public ExportResult export(ExportItem exportItem) throws ApiException {
                return null;
            }

            @Override
            protected DuplicateCheckResult concreteDuplicateCheck(PureWSResultItem pureWSResultItem, String doi,
                                                                  String title) {
                return null;
            }
        };

    @Before
    public void setUp() throws Exception {
        when(dSpaceServicesContainer.getItemService()).thenReturn(itemService);
        when(dSpaceServicesContainer.getBitstreamService()).thenReturn(bitstreamService);
        when(dSpaceServicesContainer.getResourcePolicyService()).thenReturn(resourcePolicyService);
        when(dSpaceServicesContainer.getAuthorizeService()).thenReturn(authorizeService);
    }

    @Test
    public void getItemAuthorWithPureUUID() throws SQLException {
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
        assertEquals(author, result.get(0));
    }

    @Test
    public void getItemAuthorWithoutPureUUID() throws SQLException {
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
        assertNull(result);
    }

    @Test
    public void getItemAuthorWithoutPureUUIDAllowEmpty() throws SQLException {
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
        assertNotNull(result);
    }

    @Test
    public void getItemAuthorWithoutPureUUIDAndOrcidCache() throws SQLException {
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
        assertNull(result);
    }

    @Test
    public void getAccessRightsValueForAnonymousPolicy() throws SQLException {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        Group group = mock(Group.class);

        when(group.getName()).thenReturn(Group.ANONYMOUS);
        when(resourcePolicy.getGroup()).thenReturn(group);
        when(resourcePolicyService.isDateValid(resourcePolicy)).thenReturn(true);

        AccessType accessType = classUnderTest.getAccessRightsValueForPolicies(context, List.of(resourcePolicy));
        assertEquals(AccessType.OPEN_ACCESS, accessType);
    }


    @Test
    public void getAccessRightsValueForAnonymousPolicyAndEmbargoDate() throws SQLException {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        Group group = mock(Group.class);

        Calendar c = Calendar.getInstance();
        c.setTime(new Date()); // Now use today date.
        c.add(Calendar.DATE, 15);
        Date startDate = c.getTime();

        when(group.getName()).thenReturn(Group.ANONYMOUS);
        when(resourcePolicy.getGroup()).thenReturn(group);
        when(resourcePolicyService.isDateValid(resourcePolicy)).thenReturn(false);
        when(resourcePolicy.getStartDate()).thenReturn(startDate);

        AccessType accessType = classUnderTest.getAccessRightsValueForPolicies(context, List.of(resourcePolicy));
        assertEquals(AccessType.EMBARGO, accessType);
    }

    @Test
    public void getAccessRightsValueForNonAnonymousNonAdminPolicy() throws SQLException {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        Group group = mock(Group.class);

        when(group.getName()).thenReturn("CUSTOM_GROUP");
        when(resourcePolicy.getGroup()).thenReturn(group);
        when(resourcePolicyService.isDateValid(resourcePolicy)).thenReturn(true);

        AccessType accessType = classUnderTest.getAccessRightsValueForPolicies(context, List.of(resourcePolicy));
        assertEquals(AccessType.RESTRICTED, accessType);
    }

    @Test
    public void getAccessRightsValueForNonAnonymousNonAdminPolicyAndEmbargoDate() throws SQLException {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        Group group = mock(Group.class);

        Calendar c = Calendar.getInstance();
        c.setTime(new Date()); // Now use today date.
        c.add(Calendar.DATE, 15);
        Date startDate = c.getTime();

        when(group.getName()).thenReturn("CUSTOM_GROUP");
        when(resourcePolicy.getGroup()).thenReturn(group);
        when(resourcePolicyService.isDateValid(resourcePolicy)).thenReturn(false);
        when(resourcePolicy.getStartDate()).thenReturn(startDate);

        AccessType accessType = classUnderTest.getAccessRightsValueForPolicies(context, List.of(resourcePolicy));
        assertEquals(AccessType.EMBARGO, accessType);
    }

    @Test
    public void getElectronicVersionAccessDetail() throws SQLException {
        List<Bundle> bundles = new ArrayList<>();
        Bundle bundle = mock(Bundle.class);
        bundles.add(bundle);
        Bitstream bitstreamOpenAccess = mock(Bitstream.class);
        Bitstream bitstreamEmbargo = mock(Bitstream.class);
        when(itemService.getBundles(publicationItem, org.dspace.core.Constants.DEFAULT_BUNDLE_NAME)).thenReturn(
            bundles);
        when(bundle.getPrimaryBitstream()).thenReturn(null);
        when(bundle.getBitstreams()).thenReturn(List.of(bitstreamOpenAccess, bitstreamEmbargo));
        when(classUnderTest.getAccessRightsValueForPolicies(eq(context), any(List.class))).thenReturn(
            AccessType.OPEN_ACCESS, AccessType.EMBARGO);
        when(bitstreamService.getMetadataFirstValue(bitstreamOpenAccess, "local",
                                                    "BitstreamLicense", null, Item.ANY)).thenReturn("LICENSE OA");
        when(bitstreamService.getMetadataFirstValue(bitstreamEmbargo, "local",
                                                    "BitstreamLicense", null, Item.ANY)).thenReturn("LICENSE EMBARGO");

        ElectronicVersionAccessDetail electronicVersionAccessDetail =
            classUnderTest.getElectronicVersionAccessDetail(context, publicationItem);
        assertEquals(AccessType.EMBARGO, electronicVersionAccessDetail.getAccessType());
        assertEquals("LICENSE EMBARGO", electronicVersionAccessDetail.getLicense());
    }

    @Test
    public void getElectronicVersionAccessDetailEmbargoFirst() throws SQLException {
        List<Bundle> bundles = new ArrayList<>();
        Bundle bundle = mock(Bundle.class);
        bundles.add(bundle);
        Bitstream bitstreamOpenAccess = mock(Bitstream.class);
        Bitstream bitstreamEmbargo = mock(Bitstream.class);
        when(itemService.getBundles(publicationItem, org.dspace.core.Constants.DEFAULT_BUNDLE_NAME)).thenReturn(
            bundles);
        when(bundle.getPrimaryBitstream()).thenReturn(null);
        when(bundle.getBitstreams()).thenReturn(List.of(bitstreamOpenAccess, bitstreamEmbargo));
        when(classUnderTest.getAccessRightsValueForPolicies(eq(context), any(List.class))).thenReturn(
            AccessType.EMBARGO);
        when(bitstreamService.getMetadataFirstValue(bitstreamOpenAccess, "local",
                                                    "BitstreamLicense", null, Item.ANY)).thenReturn("LICENSE EMBARGO");

        ElectronicVersionAccessDetail electronicVersionAccessDetail =
            classUnderTest.getElectronicVersionAccessDetail(context, publicationItem);
        assertEquals(AccessType.EMBARGO, electronicVersionAccessDetail.getAccessType());
        assertEquals("LICENSE EMBARGO", electronicVersionAccessDetail.getLicense());
    }

    @Test
    public void getElectronicVersionAccessDetailNoEmbargo() throws SQLException {
        List<Bundle> bundles = new ArrayList<>();
        Bundle bundle = mock(Bundle.class);
        bundles.add(bundle);
        Bitstream bitstreamOpenAccess = mock(Bitstream.class);
        Bitstream bitstreamEmbargo = mock(Bitstream.class);
        when(itemService.getBundles(publicationItem, org.dspace.core.Constants.DEFAULT_BUNDLE_NAME)).thenReturn(
            bundles);
        when(bundle.getPrimaryBitstream()).thenReturn(null);
        when(bundle.getBitstreams()).thenReturn(List.of(bitstreamOpenAccess, bitstreamEmbargo));
        when(classUnderTest.getAccessRightsValueForPolicies(eq(context), any(List.class))).thenReturn(
            AccessType.OPEN_ACCESS, AccessType.RESTRICTED);
        when(bitstreamService.getMetadataFirstValue(bitstreamOpenAccess, "local",
                                                    "BitstreamLicense", null, Item.ANY)).thenReturn("LICENSE OA");
        when(bitstreamService.getMetadataFirstValue(bitstreamEmbargo, "local",
                                                    "BitstreamLicense", null, Item.ANY)).thenReturn(
            "LICENSE RESTRICTED");

        //First non embargo access is used
        ElectronicVersionAccessDetail electronicVersionAccessDetail =
            classUnderTest.getElectronicVersionAccessDetail(context, publicationItem);
        assertEquals(AccessType.OPEN_ACCESS, electronicVersionAccessDetail.getAccessType());
        assertEquals("LICENSE OA", electronicVersionAccessDetail.getLicense());
    }

    @Test
    public void getElectronicVersionAccessDetailPrimaryBitstream() throws SQLException {
        List<Bundle> bundles = new ArrayList<>();
        Bundle bundle = mock(Bundle.class);
        bundles.add(bundle);
        Bitstream bitstream = mock(Bitstream.class);

        when(itemService.getBundles(publicationItem, org.dspace.core.Constants.DEFAULT_BUNDLE_NAME)).thenReturn(
            bundles);
        when(bundle.getPrimaryBitstream()).thenReturn(bitstream);
        when(classUnderTest.getAccessRightsValueForPolicies(eq(context), any(List.class))).thenReturn(
            AccessType.OPEN_ACCESS);
        when(bitstreamService.getMetadataFirstValue(bitstream, "local",
                                                    "BitstreamLicense", null, Item.ANY)).thenReturn("LICENSE");

        ElectronicVersionAccessDetail electronicVersionAccessDetail =
            classUnderTest.getElectronicVersionAccessDetail(context, publicationItem);
        assertEquals(AccessType.OPEN_ACCESS, electronicVersionAccessDetail.getAccessType());
        assertEquals("LICENSE", electronicVersionAccessDetail.getLicense());
    }

    @Test
    public void getItemAuthorWithOrcidCache() throws SQLException {
        String authorId = "TEST_AUTHOR_ID";
        MetadataValue authorRelation = mock(MetadataValue.class);
        when(authorRelation.getValue()).thenReturn(authorId);

        String testOrcid = UUID.randomUUID().toString();
        Map<String, Item> orcidToPureMap = new HashMap<>();
        orcidToPureMap.put(testOrcid, authorFromOrcidCache);

        when(itemService.getMetadata(publicationItem, MetadataSchemaEnum.RELATION.getName(),
                                     "isAuthorOfPublication", null, Item.ANY)).thenReturn(List.of(authorRelation));
        when(itemService.findByIdOrLegacyId(context, authorId)).thenReturn(author);
        when(itemService.getMetadataFirstValue(author, Constants.SCHEME,
                                               Constants.ELEMENT,
                                               Constants.UUID_QUALIFIER,
                                               Item.ANY)).thenReturn(null);
        when(itemService.getMetadataFirstValue(author, "person", "identifier", "orcid",
                                               Item.ANY)).thenReturn(testOrcid);
        when(dSpaceObjectMappings.getOrcidToPureMap()).thenReturn(orcidToPureMap);

        List<Item> result = classUnderTest.getItemAuthors(context, publicationItem,false);
        assertEquals(authorFromOrcidCache, result.get(0));
    }


    @Test
    public void createSearchEntityTest() {
        String title = "Title XY";
        HttpEntity<Map<String, String>> searchEntity = classUnderTest.createTitleSearchEntity(title);
        assertNotNull(searchEntity.getBody());
        assertEquals(title, searchEntity.getBody().get("searchString"));
    }
}
