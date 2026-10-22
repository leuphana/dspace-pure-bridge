package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.PressMediaApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;

import java.util.ArrayList;
import java.util.List;

public enum PressMediaMappingType {
    TYPE(api -> api.pressmediaGetAllowedTypes().getClassifications()),
    CONTRIBUTOR_ROLE(api -> api.pressMediaGetAllowedMediaCoveragesPersonsRoles().getClassifications());

    private final PurePressMediaClassificationRefFetcher pinPressMediaClassificationRefFetcher;
    private final List<ClassificationRef> classificationRefs = new ArrayList<>();

    PressMediaMappingType(PurePressMediaClassificationRefFetcher pinPressMediaClassificationRefFetcher) {
        this.pinPressMediaClassificationRefFetcher = pinPressMediaClassificationRefFetcher;
    }

    List<ClassificationRef> getClassificationRefs(PressMediaApi pressMediaApi) throws ApiException {
        if (classificationRefs.isEmpty()) {
            classificationRefs.addAll(pinPressMediaClassificationRefFetcher.fetch(pressMediaApi));
        }
        return classificationRefs;
    }
}