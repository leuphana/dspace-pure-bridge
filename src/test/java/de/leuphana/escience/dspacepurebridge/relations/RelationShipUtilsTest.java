package de.leuphana.escience.dspacepurebridge.relations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelationShipUtilsTest {

    @Mock
    Context context;

    @Mock
    RelationshipTypeService relationshipTypeService;

    @Test
    void getRelationshipTypeNotExistent() throws SQLException {
        String relationshipTypeName = "doesNotExist";
        when(relationshipTypeService.findByLeftwardOrRightwardTypeName(context, relationshipTypeName)).thenReturn(Collections.emptyList());
        Assert.assertThrows(IllegalStateException.class, () -> {
            RelationShipUtils.getRelationshipTypeForName(context, relationshipTypeService, relationshipTypeName);
        });
    }

    @Test
    void testGetRelationshipTypeForExistingEntityTypeName() throws SQLException {
        String relationshipTypeName = "isAuthorOfPublication";
        String entityTypeName = "Person";

        EntityType leftNotMatching = mock(EntityType.class);
        when(leftNotMatching.getLabel()).thenReturn("E1");
        EntityType rightNotMatching = mock(EntityType.class);
        when(rightNotMatching.getLabel()).thenReturn("E2");

        RelationshipType relationshipTypeNotMatching = mock(RelationshipType.class);
        when(relationshipTypeNotMatching.getLeftType()).thenReturn(leftNotMatching);
        when(relationshipTypeNotMatching.getRightType()).thenReturn(rightNotMatching);

        EntityType leftMatching = mock(EntityType.class);
        when(leftMatching.getLabel()).thenReturn(entityTypeName);

        RelationshipType relationshipTypeMatching = mock(RelationshipType.class);
        when(relationshipTypeMatching.getLeftType()).thenReturn(leftMatching);


        when(relationshipTypeService.findByLeftwardOrRightwardTypeName(context, relationshipTypeName)).thenReturn(
            List.of(relationshipTypeNotMatching, relationshipTypeMatching));

        RelationshipType result =
            RelationShipUtils.getRelationshipTypeForName(context, relationshipTypeService, entityTypeName,
                relationshipTypeName);
        assertEquals(relationshipTypeMatching, result);
    }

    @Test
    void testGetRelationshipTypeForNonExistingEntityTypeName() throws SQLException {
        String relationshipTypeName = "isAuthorOfPublication";
        String entityTypeName = "Person";

        EntityType left = mock(EntityType.class);
        when(left.getLabel()).thenReturn("E1");
        EntityType right = mock(EntityType.class);
        when(right.getLabel()).thenReturn("E2");

        RelationshipType relationshipType = mock(RelationshipType.class);
        when(relationshipType.getLeftType()).thenReturn(left);
        when(relationshipType.getRightType()).thenReturn(right);

        when(relationshipTypeService.findByLeftwardOrRightwardTypeName(context, relationshipTypeName)).thenReturn(
            List.of(relationshipType));

        RelationShipUtils.getRelationshipTypeForName(context, relationshipTypeService, entityTypeName,
            relationshipTypeName);

        RelationshipType result =
            RelationShipUtils.getRelationshipTypeForName(context, relationshipTypeService, entityTypeName,
                relationshipTypeName);
        assertEquals(relationshipType, result);
    }
}