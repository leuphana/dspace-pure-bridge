/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.export.identifiers;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

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
