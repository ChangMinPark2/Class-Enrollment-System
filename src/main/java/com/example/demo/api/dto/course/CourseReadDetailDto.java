package com.example.demo.api.dto.course;

import com.example.demo.api.persistence.entity.CourseStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CourseReadDetailDto(
        Long id,
        String title,
        String description,
        int price,
        int maxCapacity,
        int currentCapacity,
        LocalDate startedAt,
        LocalDate endedAt,
        CourseStatus status
) {
}
