package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.pure.apiobjects.AccessType;

public class ElectronicVersionAccessDetail {
    private AccessType accessType;
    private String license;

    public AccessType getAccessType() {
        return accessType;
    }

    public void setAccessType(AccessType accessType) {
        this.accessType = accessType;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }
}
