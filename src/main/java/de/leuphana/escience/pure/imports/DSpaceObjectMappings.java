/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.pure.imports;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.dspace.content.Item;

public class DSpaceObjectMappings {
    private static final Map<String, Item> orcidToPureMap = new HashMap<>();
    private static final Map<String, UUID> organizationNameToPureMap = new HashMap<>();

    public Map<String, Item> getOrcidToPureMap() {
        return orcidToPureMap;
    }


    public Map<String, UUID> getOrganizationNameToPureMap() {
        return organizationNameToPureMap;
    }
}
