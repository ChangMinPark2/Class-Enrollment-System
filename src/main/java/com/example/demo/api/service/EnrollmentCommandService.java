package com.example.demo.api.service;

import com.example.demo.api.dto.enrollment.EnrollmentCreateDto;
import com.example.demo.api.dto.enrollment.EnrollmentResponseDto;
import com.example.demo.api.dto.enrollment.EnrollmentResult;
import com.example.demo.api.persistence.entity.*;
import com.example.demo.api.persistence.repository.CourseRepository;
import com.example.demo.api.persistence.repository.EnrollmentRepository;
import com.example.demo.api.persistence.repository.UserRepository;
import com.example.demo.api.persistence.repository.WaitlistRepository;
import com.example.demo.error.exception.BadRequestException;
import com.example.demo.error.exception.NotFoundException;
import com.example.demo.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class EnrollmentCommandService {
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final WaitListService waitlistService;

    public EnrollmentResponseDto create(EnrollmentCreateDto dto) {
        final User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));

        final Course course = courseRepository.findById(dto.courseId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_COURSE));

        validateCourseOpen(course);
        validateAlreadyEnrolled(user, course);
        validateNotCourseOwner(user, course);

        if (isFull(course) || waitlistService.hasWaitingUser(course)) {
            return waitlistService.register(user, course);
        }

        final Enrollment enrollment = Enrollment.create(user, course);
        enrollmentRepository.save(enrollment);

        return new EnrollmentResponseDto(
                EnrollmentResult.ENROLLMENT,
                EnrollmentResult.ENROLLMENT.getMessage()
        );
    }

    public EnrollmentResponseDto confirm(Long userId, Long enrollmentId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));
        final Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_ENROLLMENT));
        final Course course = enrollment.getCourse();

        validateEnrollmentOwner(user, enrollment);
        validatePending(enrollment);

        if (waitlistService.hasWaitingUser(course) && !waitlistService.isPromotedUser(user, course)) {
            return waitlistService.registerByConfirmFailure(user, course, enrollment);
        }

        final int updatedCount = courseRepository.increaseCapacityIfAvailable(course.getId());

        if (updatedCount == 0) {
            return waitlistService.registerByConfirmFailure(user, course, enrollment);
        }

        enrollment.confirm();
        waitlistService.completeIfPromoted(user, course);

        return new EnrollmentResponseDto(
                EnrollmentResult.CONFIRMED,
                EnrollmentResult.CONFIRMED.getMessage()
        );
    }

    public void cancel(Long userId, Long enrollmentId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));

        final Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_ENROLLMENT));

        validateEnrollmentOwner(user, enrollment);
        validateConfirmed(enrollment);
        validateCancelPeriod(enrollment);

        final int cancelledCount = enrollmentRepository.cancelIfConfirmed(
                enrollmentId,
                EnrollmentStatus.CONFIRMED,
                EnrollmentStatus.CANCELLED
        );

        validateCancelSuccess(cancelledCount);

        final Course course = enrollment.getCourse();

        course.decreaseCapacity();
        waitlistService.promoteNext(course);
    }

    private boolean isFull(Course course) {
        return course.getCurrentCapacity() >= course.getMaxCapacity();
    }

    private void validateConfirmed(Enrollment enrollment) {
        if (enrollment.getEnrollmentStatus() != EnrollmentStatus.CONFIRMED) {
            throw new BadRequestException(ErrorCode.INVALID_ENROLLMENT_CANCEL_STATUS);
        }
    }

    private void validateCancelSuccess(int cancelledCount) {
        if (cancelledCount == 0) {
            throw new BadRequestException(ErrorCode.INVALID_ENROLLMENT_CANCEL_STATUS);
        }
    }

    private void validateCancelPeriod(Enrollment enrollment) {
        final LocalDateTime deadline = enrollment.getConfirmedAt().plusDays(7);

        if (deadline.isBefore(LocalDateTime.now())) {
            throw new BadRequestException(ErrorCode.INVALID_CANCEL_PERIOD);
        }
    }

    private void validateEnrollmentOwner(User user, Enrollment enrollment) {
        if (!enrollment.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(ErrorCode.INVALID_ENROLLMENT_OWNER);
        }
    }

    private void validatePending(Enrollment enrollment) {
        if (enrollment.getEnrollmentStatus() != EnrollmentStatus.PENDING) {
            throw new BadRequestException(ErrorCode.INVALID_ENROLLMENT_CONFIRM_STATUS);
        }
    }

    private void validateAlreadyEnrolled(User user, Course course) {
        if (enrollmentRepository.existsByUserAndCourse(user, course)) {
            throw new BadRequestException(ErrorCode.ALREADY_ENROLLED);
        }
    }

    private void validateNotCourseOwner(User user, Course course) {
        if (course.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(ErrorCode.INVALID_SELF_ENROLLMENT);
        }
    }

    private void validateCourseOpen(Course course) {
        if (course.getCourseStatus() != CourseStatus.OPEN) {
            throw new BadRequestException(ErrorCode.INVALID_ENROLLMENT_STATUS);
        }
    }
}
