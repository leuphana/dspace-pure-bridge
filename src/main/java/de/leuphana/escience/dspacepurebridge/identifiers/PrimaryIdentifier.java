/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge.identifiers;

import java.net.MalformedURLException;
import java.net.URL;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

public class PrimaryIdentifier {
    private final String url;
    private final String identifierBase;
    private final boolean isDoi;

    PrimaryIdentifier(Item item, MetadataValue metadataValue, boolean isDoi) {
        this.url = metadataValue.getValue();
        this.isDoi = isDoi;
        if (isDoi) {
            try {
                URL identifierUrl = new URL(url);
                this.identifierBase = identifierUrl.getPath().substring(1);
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Cannot create URL from identifier: " + metadataValue.getValue(), e);
            }
        } else {
            identifierBase = item.getHandle();
        }
    }

    public String getUrl() {
        return url;
    }

    public boolean isDoi() {
        return isDoi;
    }

    public String getIdentifierBase() {
        return identifierBase;
    }
}
