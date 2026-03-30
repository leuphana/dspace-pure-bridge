package de.leuphana.escience.dspacepurebridge.pure.export.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublicationExportFilterTest {

    @Spy
    private PublicationExportFilter publicationExportFilter;

    @Test
    public void checkForTypesToSyncWithAllowedType() {
        MetadataValue metadataValue1 = mock(MetadataValue.class);
        MetadataValue metadataValue2 = mock(MetadataValue.class);

        String allowedType1 = "dissertation";
        String allowedType2 = "text";
        when(metadataValue1.getValue()).thenReturn(allowedType2);

        String result = publicationExportFilter.checkForTypesToSync(List.of(metadataValue1, metadataValue2),
                List.of(allowedType1, allowedType2));

        assertEquals(allowedType2, result);
    }

    @Test
    public void checkForTypesToSyncWithoutAllowedType() {
        MetadataValue metadataValue1 = mock(MetadataValue.class);
        MetadataValue metadataValue2 = mock(MetadataValue.class);

        String allowedType = "dissertation";
        when(metadataValue1.getValue()).thenReturn("x");
        when(metadataValue2.getValue()).thenReturn("y");

        String result = publicationExportFilter.checkForTypesToSync(List.of(metadataValue1, metadataValue2),
                List.of(allowedType));

        Assertions.assertNull(result);
    }

    @Test
    public void checkForTypesToSyncWithoutAllowedTypeConfiguration() {
        MetadataValue metadataValue1 = mock(MetadataValue.class);
        MetadataValue metadataValue2 = mock(MetadataValue.class);

        when(metadataValue1.getValue()).thenReturn("x");

        String result = publicationExportFilter.checkForTypesToSync(List.of(metadataValue1, metadataValue2),
                Collections.emptyList());

        assertEquals("x", result);
    }

    @Test
    void buildPublicationSyncFilterFromConfigurationForCollection() {
        String collectionHandle = "TEST_COLLECTION_HANDLE";

        PublicationExportFilter publicationExportFilter =
                PublicationExportFilter.buildPublicationSyncFilterFromConfiguration("collection:" + collectionHandle);
        assertEquals(collectionHandle, publicationExportFilter.getCollectionHandle());
    }


    @Test
    void buildPublicationSyncFilterFromConfigurationForType() {
        String type1 = "type1";
        String type2 = "type2";

        PublicationExportFilter publicationExportFilter =
                PublicationExportFilter.buildPublicationSyncFilterFromConfiguration("type:" + type1 + "," + type2);
        assertEquals(List.of(type1, type2), publicationExportFilter.getTypes());
    }


    @Test
    void buildPublicationSyncFilterFromConfigurationUnknownType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            PublicationExportFilter.buildPublicationSyncFilterFromConfiguration("unknown:xy");
        });
    }

    @Test
    void itemIsSyncableForType() {
        Item item = mock(Item.class);
        ItemService itemService = mock(ItemService.class);
        String type = "dissertation";
        publicationExportFilter.getTypes().add(type);
        MetadataValue typeMetadataValue = mock(MetadataValue.class);
        when(typeMetadataValue.getValue()).thenReturn(type);
        when(itemService.getMetadataByMetadataString(item, "dc.type")).thenReturn(List.of(typeMetadataValue));

        assertEquals(type, publicationExportFilter.itemIsSyncableForType(item, itemService));
    }

    @Test
    void itemIsNotSyncableForType() {
        Item item = mock(Item.class);
        ItemService itemService = mock(ItemService.class);
        publicationExportFilter.getTypes().add("dissertation");
        MetadataValue typeMetadataValue = mock(MetadataValue.class);
        when(typeMetadataValue.getValue()).thenReturn("article");
        when(itemService.getMetadataByMetadataString(item, "dc.type")).thenReturn(List.of(typeMetadataValue));

        Assertions.assertNull(publicationExportFilter.itemIsSyncableForType(item, itemService));
    }


    @Test
    void getFilterQuerySingleType() throws SQLException {
        UUID collectionId = UUID.randomUUID();
        Context context = mock(Context.class);
        HandleService handleService = mock(HandleService.class);
        Collection collection = mock(Collection.class);
        when(collection.getID()).thenReturn(collectionId);
        when(handleService.resolveToObject(context, "TEST_COLLECTION_HANDLE")).thenReturn(collection);
        publicationExportFilter.getTypes().add("dissertation");
        publicationExportFilter.setCollectionHandle("TEST_COLLECTION_HANDLE");

        List<String> filterQueries = publicationExportFilter.getFilterQueries(context, handleService);
        assertEquals("location.coll:" + collectionId, filterQueries.get(0));
        assertEquals("itemtype_keyword:(dissertation)", filterQueries.get(1));
    }

    @Test
    void getFilterQueryMultipleTypes() throws SQLException {
        UUID collectionId = UUID.randomUUID();
        Context context = mock(Context.class);
        HandleService handleService = mock(HandleService.class);
        Collection collection = mock(Collection.class);
        when(collection.getID()).thenReturn(collectionId);
        when(handleService.resolveToObject(context, "TEST_COLLECTION_HANDLE")).thenReturn(collection);
        publicationExportFilter.getTypes().addAll(List.of("dissertation", "article"));
        publicationExportFilter.setCollectionHandle("TEST_COLLECTION_HANDLE");

        List<String> filterQueries = publicationExportFilter.getFilterQueries(context, handleService);
        assertEquals("location.coll:" + collectionId, filterQueries.get(0));
        assertEquals("itemtype_keyword:(dissertation OR article)", filterQueries.get(1));
    }
}
