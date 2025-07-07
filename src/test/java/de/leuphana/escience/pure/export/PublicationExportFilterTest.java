/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import de.leuphana.escience.pure.export.PublicationExportFilter;
import org.dspace.content.MetadataValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PublicationExportFilterTest {

    @InjectMocks
    @Spy
    private PublicationExportFilter publicationExportFilter;

    @Test
    public void checkForTypesToSyncWithAllowedType() {
        MetadataValue metadataValue1 = mock(MetadataValue.class);
        MetadataValue metadataValue2 = mock(MetadataValue.class);

        String allowedType1 = "dissertation";
        String allowedType2 = "text";
        when(metadataValue1.getValue()).thenReturn(allowedType2);

        String result = publicationExportFilter.checkForTypesToSync(List.of(metadataValue1, metadataValue2),
            List.of(allowedType1, allowedType2));

        assertEquals(allowedType2, result);
    }

    @Test
    public void checkForTypesToSyncWithoutAllowedType() {
        MetadataValue metadataValue1 = mock(MetadataValue.class);
        MetadataValue metadataValue2 = mock(MetadataValue.class);

        String allowedType = "dissertation";
        when(metadataValue1.getValue()).thenReturn("x");
        when(metadataValue2.getValue()).thenReturn("y");

        String result = publicationExportFilter.checkForTypesToSync(List.of(metadataValue1, metadataValue2),
            List.of(allowedType));

        assertNull(result);
    }

    @Test
    public void checkForTypesToSyncWithoutAllowedTypeConfiguration() {
        MetadataValue metadataValue1 = mock(MetadataValue.class);
        MetadataValue metadataValue2 = mock(MetadataValue.class);

        when(metadataValue1.getValue()).thenReturn("x");

        String result = publicationExportFilter.checkForTypesToSync(List.of(metadataValue1, metadataValue2),
            Collections.emptyList());

        assertEquals("x", result);
    }
}