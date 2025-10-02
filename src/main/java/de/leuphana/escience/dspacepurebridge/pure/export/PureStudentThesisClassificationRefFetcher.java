/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.dspacepurebridge.pure.export;

import java.util.List;

import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.StudentThesisApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;

@FunctionalInterface
public interface PureStudentThesisClassificationRefFetcher {
    List<ClassificationRef> fetch(StudentThesisApi studentThesisApi) throws ApiException;
}
