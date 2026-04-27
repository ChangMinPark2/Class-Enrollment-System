package com.example.demo.api.persistence.entity;

public enum WaitlistStatus {
    WAITING,     // 대기 중
    PROMOTED,    // 신청 기회 부여
    EXPIRED,     // 결제 시간 만료
    CANCELLED,   // 대기 취소
    COMPLETED    // 결제 완료
}
