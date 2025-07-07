/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import java.util.UUID;

public class ExportResult {
    private UUID uuid;
    private String portalUrl;

    void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }

    public String getPortalUrl() {
        return portalUrl;
    }
}
