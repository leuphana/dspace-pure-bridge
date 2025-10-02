package de.leuphana.escience.dspacepurebridge.identifiers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrimaryIdentifierHelperTest {

    @Mock
    private Item item;

    @Test
    public void primaryIdentifierWithDOIFromList() {
        String doiUrl = Identifiers.DOI_RESOLVER_HTTPS + "/10.1000/182";
        MetadataValue handleIdentifier = mock(MetadataValue.class);
        when(handleIdentifier.getValue()).thenReturn("123456789/123456789");
        MetadataValue doiIdentifier = mock(MetadataValue.class);
        when(doiIdentifier.getValue()).thenReturn(doiUrl);

        PrimaryIdentifier primaryIdentifier =
            PrimaryIdentifierHelper.getPrimaryIdentifier(item, List.of(handleIdentifier, doiIdentifier));
        assertNotNull(primaryIdentifier);
        assertTrue(primaryIdentifier.isDoi());
        assertEquals(doiUrl, primaryIdentifier.getUrl());
    }


    @Test
    public void primaryIdentifierWithoutDOIFromList() {
        String firstHandle = "123456789/123456789";
        MetadataValue handleIdentifier = mock(MetadataValue.class);
        when(handleIdentifier.getValue()).thenReturn(firstHandle);
        MetadataValue handleIdentifier2 = mock(MetadataValue.class);
        when(handleIdentifier2.getValue()).thenReturn("123456789/11111111");

        PrimaryIdentifier primaryIdentifier =
            PrimaryIdentifierHelper.getPrimaryIdentifier(item, List.of(handleIdentifier, handleIdentifier2));
        assertNotNull(primaryIdentifier);
        assertFalse(primaryIdentifier.isDoi());
        assertEquals(firstHandle, primaryIdentifier.getUrl());
    }
}