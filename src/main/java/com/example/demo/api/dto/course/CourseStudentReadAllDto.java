package com.example.demo.api.dto.course;

import java.util.List;

public record CourseStudentReadAllDto(
        List<CourseStudentReadDto> students
) {
}
