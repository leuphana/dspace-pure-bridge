package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.StudentThesisApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;

import java.util.List;

@FunctionalInterface
public interface PureStudentThesisClassificationRefFetcher {
    List<ClassificationRef> fetch(StudentThesisApi studentThesisApi) throws ApiException;
}
