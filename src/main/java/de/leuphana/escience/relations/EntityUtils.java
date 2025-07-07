/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.relations;

import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityUtils {

    private static final Logger log = LoggerFactory.getLogger(EntityUtils.class);

    protected EntityUtils() {

    }

    public static String generatePersonEntityHash(ItemService itemService, Item entity) {
        List<MetadataValue> personFamilyNameMetadata =
                itemService.getMetadata(entity, "person", "familyName", null, Item.ANY, false);
        List<MetadataValue> personGivenNameMetadata =
                itemService.getMetadata(entity, "person", "givenName", null, Item.ANY, false);
        List<MetadataValue> personOrcidMetadata =
                itemService.getMetadata(entity, "person", "identifier", "orcid", Item.ANY, false);

        return generateEntityHash(personFamilyNameMetadata, personGivenNameMetadata, personOrcidMetadata);
    }

    public static String generateProjectNameEntityHash(ItemService itemService, Item entity) {
        List<MetadataValue> projectTitleMetadataValues =
                itemService.getMetadata(entity, "dc", "title", null, Item.ANY, false);
        return EntityUtils.generateEntityHash(projectTitleMetadataValues);
    }

    public static String generateOrgUnitNameEntityHash(ItemService itemService, Item entity) {
        List<MetadataValue> orgUnitNameMetadataValues =
                itemService.getMetadata(entity, "organization", "legalName", null, Item.ANY, false);
        return EntityUtils.generateEntityHash(orgUnitNameMetadataValues);
    }

    @SafeVarargs
    public static String generateEntityHash(List<MetadataValue>... metadataValuesList) {
        StringBuilder stringToHash = new StringBuilder();
        for (List<MetadataValue> metadataValues : metadataValuesList) {
            if (metadataValues != null && !metadataValues.isEmpty() &&
                    StringUtils.isNotEmpty(metadataValues.get(0).getValue())) {
                stringToHash.append(metadataValues.get(0).getValue());
            }
        }
        log.info("Hashing: " + stringToHash);
        return DigestUtils.sha256Hex(stringToHash.toString());
    }

}
