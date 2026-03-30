package de.leuphana.escience.dspacepurebridge.pure.export.filter;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    void setCollectionHandle(String collectionHandle) {
        this.collectionHandle = collectionHandle;
    }

    String getCollectionHandle() {
        return collectionHandle;
    }

    List<String> getTypes() {
        return types;
    }

    public List<String> getFilterQueries(Context context, HandleService handleService) throws SQLException {
        List<String> filterQueries = new ArrayList<>();
        if (collectionHandle != null) {
            DSpaceObject collection = handleService.resolveToObject(context, collectionHandle);
            filterQueries.add("location.coll:" + collection.getID());
        }
        if (!types.isEmpty()) {
            filterQueries.add("itemtype_keyword:(" + String.join(" OR ", types) + ")");
        }
        return filterQueries;
    }
}
