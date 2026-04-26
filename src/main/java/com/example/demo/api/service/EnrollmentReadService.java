package com.example.demo.api.service;

import com.example.demo.api.dto.course.CourseStudentReadAllDto;
import com.example.demo.api.dto.enrollment.EnrollmentReadAllDto;
import com.example.demo.api.persistence.entity.Course;
import com.example.demo.api.persistence.entity.Enrollment;
import com.example.demo.api.persistence.entity.EnrollmentStatus;
import com.example.demo.api.persistence.entity.User;
import com.example.demo.api.persistence.repository.CourseRepository;
import com.example.demo.api.persistence.repository.EnrollmentRepository;
import com.example.demo.api.persistence.repository.UserRepository;
import com.example.demo.error.exception.BadRequestException;
import com.example.demo.error.exception.NotFoundException;
import com.example.demo.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnrollmentReadService {
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public EnrollmentReadAllDto readAllByUser(Long userId, Pageable pageable) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));

        final Page<Enrollment> enrollments = enrollmentRepository.findAllByUser(user, pageable);

        return new EnrollmentReadAllDto(
                enrollments.getContent().stream()
                        .map(Enrollment::toReadDto)
                        .toList(),
                enrollments.getNumber(),
                enrollments.getSize(),
                enrollments.getTotalElements(),
                enrollments.getTotalPages()
        );
    }

    public CourseStudentReadAllDto readStudentsByCourse(Long userId, Long courseId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_COURSE));

        validateCourseOwner(user, course);

        final List<Enrollment> enrollments =
                enrollmentRepository.findAllByCourseAndEnrollmentStatus(
                        course,
                        EnrollmentStatus.CONFIRMED
                );

        return new CourseStudentReadAllDto(
                enrollments.stream()
                        .map(Enrollment::toStudentReadDto)
                        .toList()
        );
    }

    private void validateCourseOwner(User user, Course course) {
        if (!course.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(ErrorCode.INVALID_COURSE_OWNER);
        }
    }
}
