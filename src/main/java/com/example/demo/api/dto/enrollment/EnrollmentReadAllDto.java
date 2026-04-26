package com.example.demo.api.dto.enrollment;

import java.util.List;

public record EnrollmentReadAllDto(
        List<EnrollmentReadDto> enrollments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
