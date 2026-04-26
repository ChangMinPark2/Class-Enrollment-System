package com.example.demo.api.dto.enrollment;

import com.example.demo.api.persistence.entity.EnrollmentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EnrollmentReadDto(
        Long enrollmentId,
        Long courseId,
        String courseTitle,
        int price,
        EnrollmentStatus status,
        LocalDateTime confirmedAt
) {
}
