package de.leuphana.escience.dspacepurebridge.identifiers;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrimaryIdentifierTest {

    @Mock
    Item item;

    @Mock
    MetadataValue metadataValue;

    @Test
    public void primaryIdentifierDOI() {
        String identifierBase = "doi/10.1000/182";
        when(metadataValue.getValue()).thenReturn("https://example.org/" + identifierBase);

        PrimaryIdentifier primaryIdentifier = new PrimaryIdentifier(item, metadataValue, true);

        assertEquals(identifierBase, primaryIdentifier.getIdentifierBase());
        verify(item, never()).getHandle();
    }


    @Test
    public void primaryIdentifierHandle() {
        String handle = "123456789/123456789";
        when(item.getHandle()).thenReturn(handle);

        PrimaryIdentifier primaryIdentifier = new PrimaryIdentifier(item, metadataValue, false);

        assertEquals(handle, primaryIdentifier.getIdentifierBase());
    }
}