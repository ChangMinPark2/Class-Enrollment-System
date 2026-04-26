package com.example.demo.api.dto.course;

import com.example.demo.api.persistence.entity.EnrollmentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CourseStudentReadDto(
        Long userId,
        String userName,
        Long enrollmentId,
        EnrollmentStatus status,
        LocalDateTime confirmedAt
) {
}
