package com.example.demo.api.persistence.repository;

import com.example.demo.api.persistence.entity.Course;
import com.example.demo.api.persistence.entity.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByCourseStatus(CourseStatus courseStatus);

    @Modifying(flushAutomatically = true)
    @Query("""
        update Course c
        set c.currentCapacity = c.currentCapacity + 1
        where c.id = :courseId
        and c.currentCapacity < c.maxCapacity
        """)
    int increaseCapacityIfAvailable(@Param("courseId") Long courseId);

    @Modifying(flushAutomatically = true)
    @Query("""
    update Course c
    set c.currentCapacity = c.currentCapacity - 1
    where c.id = :courseId
    and c.currentCapacity > 0
    """)
    int decreaseCapacityIfAvailable(@Param("courseId") Long courseId);
}
