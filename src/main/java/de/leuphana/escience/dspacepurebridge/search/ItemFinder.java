package de.leuphana.escience.dspacepurebridge.search;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ItemFinder {

    private static final Logger log = LoggerFactory.getLogger(ItemFinder.class);

    public DiscoverQuery buildDiscoveryQuery(String query, List<String> filterQueries, int start, int limit) {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        if (filterQueries != null && !filterQueries.isEmpty()) {
            discoverQuery.addFilterQueries(filterQueries.toArray(String[]::new));
        }
        discoverQuery.setQuery(query);
        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);
        discoverQuery.setSortField("search.resourceid", DiscoverQuery.SORT_ORDER.asc);
        return discoverQuery;
    }

    public Iterator<Item> findItems(Context context, SearchService searchService, SearchQueryType searchQueryType, List<String> additionalFilterQueries, int start, int limit) throws SearchServiceException {
        log.info("Searching for items (query: {}, filterQuery: {}, additionalFilterQueries: {},  start: {}, limit: {})", searchQueryType.getQuery(), searchQueryType.getFilter(), additionalFilterQueries, start, limit);

        List<String> filterQueries = new ArrayList<>();
        if (searchQueryType.getFilter() != null) {
            filterQueries.add(searchQueryType.getFilter());
        }
        if (additionalFilterQueries != null && !additionalFilterQueries.isEmpty()) {
            filterQueries.addAll(additionalFilterQueries);
        }

        DiscoverQuery discoverQuery = buildDiscoveryQuery(searchQueryType.getQuery(), filterQueries, start, limit);

        return searchService.search(context, discoverQuery).getIndexableObjects()
                .stream()
                .map(indexableObject ->
                        ((IndexableItem) indexableObject).getIndexedObject())
                .collect(Collectors.toList())
                .iterator();
    }

    public void processAllItems(Context context, SearchService searchService, SearchQueryType searchQueryType, List<String> additionalFilterQueries, ItemProcessor processor) throws SearchServiceException {
        int start = 0;
        int limit = 100;
        processAllItems(context, searchService, searchQueryType, additionalFilterQueries, processor, start, limit);
    }

    public void processAllItems(Context context, SearchService searchService, SearchQueryType searchQueryType, ItemProcessor processor) throws SearchServiceException {
        int start = 0;
        int limit = 100;
        processAllItems(context, searchService, searchQueryType, null, processor, start, limit);
    }

    void processAllItems(Context context, SearchService searchService, SearchQueryType searchQueryType, ItemProcessor processor, int start, int limit) throws SearchServiceException {
        processAllItems(context, searchService, searchQueryType, null, processor, start, limit);
    }
    void processAllItems(Context context, SearchService searchService, SearchQueryType searchQueryType, List<String> additionalFilterQueries, ItemProcessor processor, int start, int limit) throws SearchServiceException {
        int counter = 0;
        Iterator<Item> itemIterator = findItems(context, searchService, searchQueryType, additionalFilterQueries, start, limit);
        while (itemIterator != null && itemIterator.hasNext()) {
            Item item = itemIterator.next();

            processor.process(item);

            counter++;

            if (counter == limit) {
                counter = 0;
                start += limit;
                itemIterator = findItems(context, searchService, searchQueryType, additionalFilterQueries, start, limit);
            }
        }
    }
}
