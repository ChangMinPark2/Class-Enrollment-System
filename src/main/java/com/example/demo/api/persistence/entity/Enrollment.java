package com.example.demo.api.persistence.entity;

import com.example.demo.api.dto.course.CourseStudentReadDto;
import com.example.demo.api.dto.enrollment.EnrollmentReadDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tbl_enrollment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {
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

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EnrollmentStatus enrollmentStatus;

    @Builder
    private Enrollment(User user, Course course, EnrollmentStatus enrollmentStatus, LocalDateTime confirmedAt) {
        this.user = user;
        this.course = course;
        this.enrollmentStatus = enrollmentStatus;
        this.confirmedAt = confirmedAt;
    }

    public static Enrollment create(User user, Course course) {
        return Enrollment.builder()
                .user(user)
                .course(course)
                .enrollmentStatus(EnrollmentStatus.PENDING)
                .confirmedAt(null)
                .build();
    }

    public EnrollmentReadDto toReadDto() {
        return EnrollmentReadDto.builder()
                .enrollmentId(this.id)
                .courseId(this.course.getId())
                .courseTitle(this.course.getTitle())
                .price(this.course.getPrice())
                .status(this.enrollmentStatus)
                .confirmedAt(this.confirmedAt)
                .build();
    }

    public CourseStudentReadDto toStudentReadDto() {
        return CourseStudentReadDto.builder()
                .userId(this.user.getId())
                .userName(this.user.getName())
                .enrollmentId(this.id)
                .status(this.enrollmentStatus)
                .confirmedAt(this.confirmedAt)
                .build();
    }

    public void confirm() {
        this.enrollmentStatus = EnrollmentStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
}

