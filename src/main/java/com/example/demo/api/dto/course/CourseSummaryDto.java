package com.example.demo.api.dto.course;

import com.example.demo.api.persistence.entity.CourseStatus;
import lombok.Builder;

@Builder
public record CourseSummaryDto(
        Long id,
        String title,
        int price,
        int maxCapacity,
        int currentCapacity,
        CourseStatus status
) {
}
