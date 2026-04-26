package com.example.demo.api.persistence.repository;

import com.example.demo.api.persistence.entity.Course;
import com.example.demo.api.persistence.entity.Enrollment;
import com.example.demo.api.persistence.entity.EnrollmentStatus;
import com.example.demo.api.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserAndCourse(User user, Course course);

    @Modifying(flushAutomatically = true)
    @Query("""
        update Enrollment e
        set e.enrollmentStatus = :cancelledStatus
        where e.id = :enrollmentId
        and e.enrollmentStatus = :confirmedStatus
        """)
    int cancelIfConfirmed(
            @Param("enrollmentId") Long enrollmentId,
            @Param("confirmedStatus") EnrollmentStatus confirmedStatus,
            @Param("cancelledStatus") EnrollmentStatus cancelledStatus
    );
}
