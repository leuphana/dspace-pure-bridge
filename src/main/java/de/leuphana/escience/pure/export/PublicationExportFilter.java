/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationExportFilter {
    private String collectionHandle = null;
    private final List<String> types = new ArrayList<>();

    private static final Logger log = LoggerFactory.getLogger(PublicationExportFilter.class);

    private PublicationExportFilter() {
    }

    public static PublicationExportFilter buildPublicationSyncFilterFromConfiguration(String configurationString) {
        if (configurationString == null) {
            throw new IllegalArgumentException("Configuration must not be null!");
        }
        PublicationExportFilter publicationExportFilter = new PublicationExportFilter();
        log.info("Registering filter for configuration:  {}", configurationString);
        for (String configuration : configurationString.split(";")) {
            String[] configurationPart = configuration.split(":");
            if (configurationPart.length > 1) {
                switch (configurationPart[0]) {
                    case "collection":
                        publicationExportFilter.collectionHandle = configurationPart[1];
                        break;
                    case "type":
                        publicationExportFilter.types.addAll(Arrays.asList(configurationPart[1].split(",")));
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected configuration key: " + configurationPart[0]);
                }
            }
        }
        return publicationExportFilter;
    }

    public String itemIsSyncableForType(Item item, ItemService itemService) {
        if (collectionHandle != null && ! collectionHandle.equals(item.getOwningCollection().getHandle())) {
            return null;
        }
        List<MetadataValue> typeMetadataValues = itemService.getMetadataByMetadataString(item, "dc.type");
        return checkForTypesToSync(typeMetadataValues, types);
    }

    String checkForTypesToSync(List<MetadataValue> metadataValues, List<String> allowedTypes) {
        if (metadataValues.isEmpty()) {
            return null;
        }
        for (MetadataValue metadataValue : metadataValues) {
            if (allowedTypes.isEmpty() || allowedTypes.contains(metadataValue.getValue())) {
                return metadataValue.getValue();
            }
        }
        return null;
    }
}
