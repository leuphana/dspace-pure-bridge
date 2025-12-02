package de.leuphana.escience.dspacepurebridge.pure.imports;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DSpaceObjectMappings {
    private static final Map<String, UUID> organizationNameToPureMap = new HashMap<>();

    public Map<String, UUID> getOrganizationNameToPureMap() {
        return organizationNameToPureMap;
    }
}
