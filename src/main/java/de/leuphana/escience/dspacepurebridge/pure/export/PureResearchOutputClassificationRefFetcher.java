package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.ResearchOutputApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;

import java.util.List;

@FunctionalInterface
public interface PureResearchOutputClassificationRefFetcher {
    List<ClassificationRef> fetch(ResearchOutputApi researchOutputApi) throws ApiException;
}
