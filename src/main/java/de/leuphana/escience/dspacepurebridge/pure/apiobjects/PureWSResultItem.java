package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

import java.util.UUID;

public abstract class PureWSResultItem {
    private long pureId;
    private String portalUrl;
    private UUID uuid;
    private String modifiedDate;
    private Visibility visibility;

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public long getPureId() {
        return pureId;
    }

    public void setPureId(long pureId) {
        this.pureId = pureId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public String getPortalUrl() {
        return portalUrl;
    }

    public void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }
}
