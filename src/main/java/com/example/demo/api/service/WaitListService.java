package com.example.demo.api.service;

import com.example.demo.api.dto.enrollment.EnrollmentResponseDto;
import com.example.demo.api.dto.enrollment.EnrollmentResult;
import com.example.demo.api.persistence.entity.*;
import com.example.demo.api.persistence.repository.EnrollmentRepository;
import com.example.demo.api.persistence.repository.WaitlistRepository;
import com.example.demo.error.exception.BadRequestException;
import com.example.demo.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class WaitListService {
    private final WaitlistRepository waitlistRepository;
    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentResponseDto register(User user, Course course) {
        validateAlreadyWaiting(user, course);

        final Waitlist waitlist = Waitlist.create(user, course);
        waitlistRepository.save(waitlist);

        return new EnrollmentResponseDto(
                EnrollmentResult.WAITLIST,
                EnrollmentResult.WAITLIST.getMessage()
        );
    }

    public EnrollmentResponseDto registerByConfirmFailure(
            User user,
            Course course,
            Enrollment enrollment
    ) {
        validateAlreadyWaiting(user, course);
        enrollment.cancel();

        final Waitlist waitlist = Waitlist.create(user, course);
        waitlistRepository.save(waitlist);

        return new EnrollmentResponseDto(
                EnrollmentResult.WAITLIST,
                EnrollmentResult.WAITLIST.getMessage()
        );
    }

    public void promoteNext(Course course) {
        final Optional<Waitlist> optionalWaitlist =
                waitlistRepository.findFirstByCourseAndWaitlistStatusOrderByCreatedAtAsc(
                        course,
                        WaitlistStatus.WAITING
                );

        if (optionalWaitlist.isEmpty()) {
            return;
        }

        final Waitlist waitlist = optionalWaitlist.get();
        final Enrollment enrollment = Enrollment.create(
                waitlist.getUser(),
                course
        );

        enrollmentRepository.save(enrollment);

        waitlist.promote(LocalDateTime.now().plusMinutes(10));
    }

    public boolean isPromotedUser(User user, Course course) {
        return waitlistRepository.existsByUserAndCourseAndWaitlistStatus(
                user,
                course,
                WaitlistStatus.PROMOTED
        );
    }

    public void completeIfPromoted(User user, Course course) {
        waitlistRepository.findByUserAndCourseAndWaitlistStatus(
                user,
                course,
                WaitlistStatus.PROMOTED
        ).ifPresent(Waitlist::complete);
    }

    public boolean hasWaitingUser(Course course) {
        return waitlistRepository.existsByCourseAndWaitlistStatus(
                course,
                WaitlistStatus.WAITING
        );
    }

    private void validateAlreadyWaiting(User user, Course course) {
        if (waitlistRepository.existsByUserAndCourseAndWaitlistStatus(
                user,
                course,
                WaitlistStatus.WAITING
        )) {
            throw new BadRequestException(ErrorCode.ALREADY_WAITING);
        }
    }
}

