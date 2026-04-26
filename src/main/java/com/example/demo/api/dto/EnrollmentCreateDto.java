package com.example.demo.api.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentCreateDto(
        @NotNull(message = "유저 ID는 필수입니다.")
        Long userId,

        @NotNull(message = "강의 ID는 필수입니다.")
        Long courseId
) {
}
