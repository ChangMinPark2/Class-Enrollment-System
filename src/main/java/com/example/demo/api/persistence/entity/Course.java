package com.example.demo.api.persistence.entity;

import com.example.demo.api.dto.course.CourseSummaryDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "tbl_course")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "max_capacity", nullable = false)
    private int maxCapacity;

    @Column(name = "current_capacity", nullable = false)
    private int currentCapacity;

    @Column(name = "started_at", updatable = false, nullable = false)
    private LocalDate startedAt;

    @Column(name = "ended_at", updatable = false, nullable = false)
    private LocalDate endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CourseStatus courseStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private Course(
            String title,
            String description,
            int price,
            int maxCapacity,
            int currentCapacity,
            LocalDate startedAt,
            LocalDate endedAt,
            CourseStatus status,
            User user
    ) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.maxCapacity = maxCapacity;
        this.currentCapacity = currentCapacity;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.courseStatus = status;
        this.user = user;
    }

    public static Course create(
            String title,
            String description,
            int price,
            int maxCapacity,
            LocalDate startedAt,
            LocalDate endedAt,
            User user
    ) {
        return Course.builder()
                .title(title)
                .description(description)
                .price(price)
                .maxCapacity(maxCapacity)
                .currentCapacity(0)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .status(CourseStatus.DRAFT)
                .user(user)
                .build();
    }

    public CourseSummaryDto toSummaryDto() {
        return CourseSummaryDto.builder()
                .id(this.id)
                .title(this.title)
                .price(this.price)
                .maxCapacity(this.maxCapacity)
                .currentCapacity(this.currentCapacity)
                .status(this.courseStatus)
                .build();
    }

    public void open() {
        this.courseStatus = CourseStatus.OPEN;
    }
}
