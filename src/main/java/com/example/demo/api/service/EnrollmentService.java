package com.example.demo.api.service;

import com.example.demo.api.dto.EnrollmentCreateDto;
import com.example.demo.api.persistence.entity.Course;
import com.example.demo.api.persistence.entity.CourseStatus;
import com.example.demo.api.persistence.entity.Enrollment;
import com.example.demo.api.persistence.entity.User;
import com.example.demo.api.persistence.repository.CourseRepository;
import com.example.demo.api.persistence.repository.EnrollmentRepository;
import com.example.demo.api.persistence.repository.UserRepository;
import com.example.demo.error.exception.BadRequestException;
import com.example.demo.error.exception.NotFoundException;
import com.example.demo.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public void create(EnrollmentCreateDto dto) {
        final User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));
        final Course course = courseRepository.findById(dto.courseId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_COURSE));

        validateCourseOpen(course);
        validateCapacity(course);
        validateAlreadyEnrolled(user, course);
        validateNotCourseOwner(user, course);

        final Enrollment enrollment = Enrollment.create(user, course);

        enrollmentRepository.save(enrollment);
    }

    private void validateAlreadyEnrolled(User user, Course course) {
        if (enrollmentRepository.existsByUserAndCourse(user, course)) {
            throw new BadRequestException(ErrorCode.ALREADY_ENROLLED);
        }
    }

    private void validateNotCourseOwner(User user, Course course) {
        if (course.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(ErrorCode.INVALID_ENROLLMENT_OWNER);
        }
    }

    private void validateCourseOpen(Course course) {
        if (course.getCourseStatus() != CourseStatus.OPEN) {
            throw new BadRequestException(ErrorCode.INVALID_ENROLLMENT_STATUS);
        }
    }

    private void validateCapacity(Course course) {
        if (course.getCurrentCapacity() >= course.getMaxCapacity()) {
            throw new BadRequestException(ErrorCode.INVALID_COURSE_CAPACITY);
        }
    }
}
