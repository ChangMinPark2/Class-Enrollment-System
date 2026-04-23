package com.example.demo.api.persistence.repository;

import com.example.demo.api.persistence.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
