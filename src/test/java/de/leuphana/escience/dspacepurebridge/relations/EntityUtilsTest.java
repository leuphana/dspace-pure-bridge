package de.leuphana.escience.dspacepurebridge.relations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityUtilsTest {

    @Mock
    Item item;

    @Mock
    ItemService itemService;

    @Test
    void generatePersonEntityHash() {
        String lastName = "lastName";
        String firstName = "firstName";
        String orcid = "xxxx-xxxx-xxxx-xxxx";
        String expected =  DigestUtils.sha256Hex(lastName + firstName + orcid);

        MetadataValue lastNameMetadata = mock(MetadataValue.class);
        when(lastNameMetadata.getValue()).thenReturn(lastName);
        MetadataValue firstNameMetadata = mock(MetadataValue.class);
        when(firstNameMetadata.getValue()).thenReturn(firstName);
        MetadataValue orcidMetadata = mock(MetadataValue.class);
        when(orcidMetadata.getValue()).thenReturn(orcid);
        when(itemService.getMetadata(item, "person", "familyName", null, Item.ANY, false)).thenReturn(List.of(lastNameMetadata));
        when(itemService.getMetadata(item, "person", "givenName", null, Item.ANY, false)).thenReturn(List.of(firstNameMetadata));
        when(itemService.getMetadata(item, "person", "identifier", "orcid", Item.ANY, false)).thenReturn(List.of(orcidMetadata));

        String result = EntityUtils.generatePersonEntityHash(itemService, item);
        assertEquals(expected, result);
    }

    @Test
    void generateProjectNameEntityHash() {
        String projectName = "project";
        String expected =  DigestUtils.sha256Hex(projectName);

        MetadataValue projectNameMetadata = mock(MetadataValue.class);
        when(projectNameMetadata.getValue()).thenReturn(projectName);
        when(itemService.getMetadata(item, "dc", "title", null, Item.ANY, false)).thenReturn(List.of(projectNameMetadata));

        String result = EntityUtils.generateProjectNameEntityHash(itemService, item);
        assertEquals(expected, result);
    }

    @Test
    void generateOrgUnitNameEntityHash() {
        String orgUnitName = "orgunit";
        String expected =  DigestUtils.sha256Hex(orgUnitName);

        MetadataValue orgUnitNameMetadata = mock(MetadataValue.class);
        when(orgUnitNameMetadata.getValue()).thenReturn(orgUnitName);
        when(itemService.getMetadata(item, "organization", "legalName", null, Item.ANY, false)).thenReturn(List.of(orgUnitNameMetadata));

        String result = EntityUtils.generateOrgUnitNameEntityHash(itemService, item);
        assertEquals(expected, result);
    }

    @Test
    void generateEntityHash() {
    }
}