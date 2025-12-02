package de.leuphana.escience.dspacepurebridge.pure.export;

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
