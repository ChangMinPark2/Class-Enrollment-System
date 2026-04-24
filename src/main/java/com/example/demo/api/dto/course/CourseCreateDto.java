package com.example.demo.api.dto.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CourseCreateDto(
        @NotBlank(message = "강의 제목은 필수입니다.")
        @Size(max = 100, message = "강의 제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "강의 설명은 필수입니다.")
        @Size(max = 1000, message = "강의 설명은 1000자 이하여야 합니다.")
        String description,

        @NotNull(message = "가격은 필수입니다.")
        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        Integer price,

        @NotNull(message = "정원은 필수입니다.")
        @Min(value = 1, message = "정원은 최소 1명 이상이어야 합니다.")
        Integer maxCapacity,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startedAt,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endedAt
) {
}
