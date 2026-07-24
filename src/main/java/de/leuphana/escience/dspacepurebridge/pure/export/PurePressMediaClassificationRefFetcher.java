package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.PressMediaApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;

import java.util.List;

@FunctionalInterface
public interface PurePressMediaClassificationRefFetcher {
    List<ClassificationRef> fetch(PressMediaApi pressMediaApi) throws ApiException;
}