/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import java.util.List;

import de.leuphana.escience.pure.generated.ApiException;
import de.leuphana.escience.pure.generated.api.ResearchOutputApi;
import de.leuphana.escience.pure.generated.model.ClassificationRef;

@FunctionalInterface
public interface PureResearchOutputClassificationRefFetcher {
    List<ClassificationRef> fetch(ResearchOutputApi researchOutputApi) throws ApiException;
}
