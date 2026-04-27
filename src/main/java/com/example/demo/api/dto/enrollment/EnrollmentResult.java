package com.example.demo.api.dto.enrollment;

import lombok.Getter;

@Getter
public enum EnrollmentResult {
    ENROLLMENT("수강 신청이 완료되었습니다. 결제를 진행해주세요."),
    WAITLIST("대기열에 등록되었습니다. 빈 자리가 생기면 수강 신청 기회가 제공됩니다."),
    CONFIRMED("결제가 완료되어 수강이 확정되었습니다.");

    private final String message;

    EnrollmentResult(String message) {
        this.message = message;
    }
}
