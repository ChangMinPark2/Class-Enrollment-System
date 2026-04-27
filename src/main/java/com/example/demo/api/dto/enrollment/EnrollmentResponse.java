package com.example.demo.api.dto.enrollment;

import lombok.Getter;

@Getter
public enum EnrollmentResponse {
    ENROLLMENT("수강 신청이 완료되었습니다. 결제를 진행해주세요."),
    WAITLIST("대기열에 등록되었습니다. 빈 자리가 생기면 수강 신청 기회가 제공됩니다.");

    private final String message;

    EnrollmentResponse(String message) {
        this.message = message;
    }
}
