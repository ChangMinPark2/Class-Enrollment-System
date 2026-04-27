package com.example.demo.api.dto.enrollment;

public record EnrollmentResponseDto(
        EnrollmentResult type,
        String message
) {
}
