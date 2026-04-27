package com.example.demo.api.persistence.repository;

import com.example.demo.api.persistence.entity.Course;
import com.example.demo.api.persistence.entity.User;
import com.example.demo.api.persistence.entity.Waitlist;
import com.example.demo.api.persistence.entity.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    boolean existsByUserAndCourseAndWaitlistStatus(
            User user,
            Course course,
            WaitlistStatus waitlistStatus
    );

    boolean existsByCourseAndWaitlistStatus(
            Course course,
            WaitlistStatus waitlistStatus
    );
}
