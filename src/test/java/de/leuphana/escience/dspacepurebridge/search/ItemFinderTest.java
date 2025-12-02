package de.leuphana.escience.dspacepurebridge.search;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ItemFinderTest {

    @Mock
    Context context;

    @Mock
    SearchService searchService;

    @Spy
    ItemFinder itemFinder;

    @Test
    void testProcessAllItemsPublic() throws SearchServiceException {
        Item item1 = mock(Item.class);
        Item item2 = mock(Item.class);
        Set<Item> itemSet = new HashSet<>();
        SearchQueryType queryType = mock(SearchQueryType.class);
        ItemProcessor processor = itemSet::add;
        doReturn(List.of(item1, item2).iterator()).when(itemFinder).findItems(context, searchService, queryType, 0 , 100);

        itemFinder.processAllItems(context, searchService, queryType, processor);

        assertTrue(itemSet.containsAll(List.of(item1, item2)));
    }

    @Test
    void testProcessAllItems() throws SearchServiceException {
        Item item1 = mock(Item.class);
        Item item2 = mock(Item.class);
        Set<Item> itemSet = new HashSet<>();
        SearchQueryType queryType = mock(SearchQueryType.class);
        ItemProcessor processor = itemSet::add;
        doReturn(List.of(item1).iterator()).when(itemFinder).findItems(context, searchService, queryType, 0 , 1);
        doReturn(List.of(item2).iterator()).when(itemFinder).findItems(context, searchService, queryType, 1 , 1);
        doReturn(Collections.emptyIterator()).when(itemFinder).findItems(context, searchService, queryType, 2 , 1);

        itemFinder.processAllItems(context, searchService, queryType, processor, 0 , 1);

        assertTrue(itemSet.containsAll(List.of(item1, item2)));
    }

    @Test
    void buildDiscoveryQueryWithFilter() {
        DiscoverQuery discoverQuery = itemFinder.buildDiscoveryQuery("field:value", "filter:xy", 1, 2);
        assertEquals("field:value", discoverQuery.getQuery());
        assertEquals(List.of("filter:xy"), discoverQuery.getFilterQueries());
        assertEquals(1,discoverQuery.getStart());
        assertEquals(2,discoverQuery.getMaxResults());
    }

    @Test
    void buildDiscoveryQueryWithoutFilter() {
        DiscoverQuery discoverQuery = itemFinder.buildDiscoveryQuery("field:value", null, 1, 2);
        assertEquals("field:value", discoverQuery.getQuery());
        assertEquals(Collections.emptyList(), discoverQuery.getFilterQueries());
        assertEquals(1,discoverQuery.getStart());
        assertEquals(2,discoverQuery.getMaxResults());
    }
}