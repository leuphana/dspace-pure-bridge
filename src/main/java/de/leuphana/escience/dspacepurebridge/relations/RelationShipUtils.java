package de.leuphana.escience.dspacepurebridge.relations;

import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class RelationShipUtils {
    private static final Logger log = LoggerFactory.getLogger(RelationShipUtils.class);

    private RelationShipUtils() {
    }

    public static RelationshipType getRelationshipTypeForName(Context context,
                                                              RelationshipTypeService relationshipTypeService,
                                                              String relationshipName) throws SQLException {
        return getRelationshipTypeForName(context, relationshipTypeService, null, relationshipName);
    }

    public static RelationshipType getRelationshipTypeForName(Context context,
                                                              RelationshipTypeService relationshipTypeService,
                                                              String entityTypeName, String relationshipName)
        throws SQLException {
        List<RelationshipType> relationshipTypes =
            relationshipTypeService.findByLeftwardOrRightwardTypeName(context, relationshipName);
        if (relationshipTypes.isEmpty()) {
            throw new IllegalStateException("Could not find required Relationship: " + relationshipName);
        } else if (entityTypeName != null) {
            for (RelationshipType relationshipType : relationshipTypes) {
                if (relationshipType.getLeftType().getLabel().equals(entityTypeName) ||
                    relationshipType.getRightType().getLabel().equals(entityTypeName)) {
                    return relationshipType;
                }
            }
        }
        return relationshipTypes.get(0);
    }
}
