package com.example.demo.api.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tbl_waitlist")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Waitlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "waitlist_status", nullable = false)
    private WaitlistStatus waitlistStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder
    private Waitlist(
            User user,
            Course course,
            WaitlistStatus waitlistStatus,
            LocalDateTime createdAt,
            LocalDateTime promotedAt,
            LocalDateTime expiresAt
    ) {
        this.user = user;
        this.course = course;
        this.waitlistStatus = waitlistStatus;
        this.createdAt = createdAt;
        this.promotedAt = promotedAt;
        this.expiresAt = expiresAt;
    }

    // 생성
    public static Waitlist create(User user, Course course) {
        return Waitlist.builder()
                .user(user)
                .course(course)
                .waitlistStatus(WaitlistStatus.WAITING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 승격
    public void promote(LocalDateTime expiresAt) {
        this.waitlistStatus = WaitlistStatus.PROMOTED;
        this.promotedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    // 만료
    public void expire() {
        this.waitlistStatus = WaitlistStatus.EXPIRED;
    }

    // 취소
    public void cancel() {
        this.waitlistStatus = WaitlistStatus.CANCELLED;
    }

    // 완료 (결제 성공 시)
    public void complete() {
        this.waitlistStatus = WaitlistStatus.COMPLETED;
    }
}
