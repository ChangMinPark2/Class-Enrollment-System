package com.example.demo.api.dto.course;

import java.util.List;

public record CourseReadAllDto(
        List<CourseSummaryDto> courses
) {
}
