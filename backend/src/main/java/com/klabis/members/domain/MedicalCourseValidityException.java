package com.klabis.members.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

public class MedicalCourseValidityException extends InvalidDataException implements UserFacingError {

    public MedicalCourseValidityException() {
        super("Medical course validity date must be after its completion date");
    }

    @Override
    public String errorType() {
        return "medical-course-validity-not-after-completion";
    }

    @Override
    public String field() {
        return "medicalCourse";
    }
}
