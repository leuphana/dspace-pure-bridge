package de.leuphana.escience.dspacepurebridge.pure.export;

import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.StudentThesisApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;

import java.util.ArrayList;
import java.util.List;

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
