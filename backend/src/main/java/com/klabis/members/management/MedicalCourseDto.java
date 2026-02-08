package com.klabis.members.management;

import com.klabis.members.MedicalCourse;

import java.time.LocalDate;
import java.util.Optional;

/**
 * DTO for medical course information.
 * <p>
 * This DTO is used to transfer medical course data between application and presentation layers.
 * It represents the same data as the domain {@link MedicalCourse} value object.
 * <p>
 * Some medical courses have indefinite validity, in which case validityDate will be null.
 * Jackson serializes LocalDate in ISO-8601 format (e.g., "2026-01-17").
 * <p>
 * For JSON serialization, Jackson handles Optional&lt;LocalDate&gt; as:
 * - Present: {"completionDate": "2026-01-01", "validityDate": "2026-12-31"}
 * - Empty: {"completionDate": "2026-01-01", "validityDate": null}
 *
 * @param completionDate course completion date (ISO-8601 format)
 * @param validityDate   course validity date (ISO-8601 format, or null if indefinite)
 */
record MedicalCourseDto(
        LocalDate completionDate,
        Optional<LocalDate> validityDate
) {
}
