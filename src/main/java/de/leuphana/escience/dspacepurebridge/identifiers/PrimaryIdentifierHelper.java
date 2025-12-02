package de.leuphana.escience.dspacepurebridge.identifiers;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

import java.util.List;

public class PrimaryIdentifierHelper {
    private PrimaryIdentifierHelper() {
    }

    public static PrimaryIdentifier getPrimaryIdentifier(Item item, List<MetadataValue> identifiers) {
        for (MetadataValue metadataValue : identifiers) {
            if (metadataValue.getValue().startsWith(Identifiers.DOI_RESOLVER_HTTPS) ||
                metadataValue.getValue().startsWith(Identifiers.DOI_RESOLVER_HTTP)) {
                return new PrimaryIdentifier(item, metadataValue, true);
            }
        }
        if (!identifiers.isEmpty()) {
            return new PrimaryIdentifier(item, identifiers.get(0), false);
        }
        return null;
    }
}
