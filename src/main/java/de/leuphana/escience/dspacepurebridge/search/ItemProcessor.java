package de.leuphana.escience.dspacepurebridge.search;

import org.dspace.content.Item;
import org.dspace.discovery.SearchServiceException;

@FunctionalInterface
public interface ItemProcessor {
    void process(Item item) throws SearchServiceException;
}
