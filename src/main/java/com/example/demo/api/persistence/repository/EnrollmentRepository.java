package com.example.demo.api.persistence.repository;

import com.example.demo.api.persistence.entity.Course;
import com.example.demo.api.persistence.entity.Enrollment;
import com.example.demo.api.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserAndCourse(User user, Course course);
}
