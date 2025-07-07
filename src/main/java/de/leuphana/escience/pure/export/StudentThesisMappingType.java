/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import java.util.ArrayList;
import java.util.List;

import de.leuphana.escience.pure.generated.ApiException;
import de.leuphana.escience.pure.generated.api.StudentThesisApi;
import de.leuphana.escience.pure.generated.model.ClassificationRef;

public enum StudentThesisMappingType {
    TYPE(api -> api.studentThesisGetAllowedTypes().getClassifications()),
    LANGUAGE(api -> api.studentthesisGetAllowedLanguages().getClassifications()),
    CONTRIBUTOR_ROLE(api -> api.studentthesisGetAllowedContributorRoles().getClassifications()),
    SUPERVISOR_ROLE(api -> api.studentthesisGetAllowedSupervisorRoles().getClassifications());

    private final PureStudentThesisClassificationRefFetcher pureStudentThesisClassificationRefFetcher;
    private final List<ClassificationRef> classificationRefs = new ArrayList<>();

    StudentThesisMappingType(PureStudentThesisClassificationRefFetcher pureStudentThesisClassificationRefFetcher) {
        this.pureStudentThesisClassificationRefFetcher = pureStudentThesisClassificationRefFetcher;
    }

    List<ClassificationRef> getClassificationRefs(StudentThesisApi studentThesisApi) throws ApiException {
        if (classificationRefs.isEmpty()) {
            classificationRefs.addAll(pureStudentThesisClassificationRefFetcher.fetch(studentThesisApi));
        }
        return classificationRefs;
    }
}
