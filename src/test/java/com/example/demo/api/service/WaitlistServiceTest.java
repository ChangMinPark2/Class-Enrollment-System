package com.example.demo.api.service;

import com.example.demo.api.dto.enrollment.EnrollmentResponseDto;
import com.example.demo.api.dto.enrollment.EnrollmentResult;
import com.example.demo.error.exception.BadRequestException;
import com.example.demo.api.persistence.entity.*;
import com.example.demo.api.persistence.repository.EnrollmentRepository;
import com.example.demo.api.persistence.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WaitlistServiceTest {
    @Mock
    private WaitlistRepository waitlistRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private WaitListService waitListService;

    private User creator;
    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        creator = createUser(1L, "강사", Role.CREATOR);
        student = createUser(2L, "학생", Role.STUDENT);
        course = createOpenCourse(10L, creator);
    }

    @Test
    @DisplayName("대기열 등록 성공")
    void register_success() {
        // Given
        when(waitlistRepository.existsByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.WAITING
        )).thenReturn(false);

        // When
        EnrollmentResponseDto response = waitListService.register(student, course);

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.WAITLIST);
        assertThat(response.message()).isEqualTo(EnrollmentResult.WAITLIST.getMessage());

        ArgumentCaptor<Waitlist> captor = ArgumentCaptor.forClass(Waitlist.class);
        verify(waitlistRepository).save(captor.capture());

        Waitlist savedWaitlist = captor.getValue();
        assertThat(savedWaitlist.getUser()).isEqualTo(student);
        assertThat(savedWaitlist.getCourse()).isEqualTo(course);
        assertThat(savedWaitlist.getWaitlistStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(savedWaitlist.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("대기열 등록 실패 - 이미 WAITING 상태로 대기 중이면 예외가 발생한다")
    void register_fail_already_waiting() {
        // Given
        when(waitlistRepository.existsByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.WAITING
        )).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> waitListService.register(student, course))
                .isInstanceOf(BadRequestException.class);

        verify(waitlistRepository, never()).save(any(Waitlist.class));
    }

    @Test
    @DisplayName("수강 확정 실패로 대기열 등록 성공 - 기존 수강 신청은 취소되고 대기열에 등록된다")
    void registerByConfirmFailure_success() {
        // Given
        Enrollment enrollment = createPendingEnrollment(100L, student, course);

        when(waitlistRepository.existsByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.WAITING
        )).thenReturn(false);

        // When
        EnrollmentResponseDto response =
                waitListService.registerByConfirmFailure(student, course, enrollment);

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.WAITLIST);
        assertThat(response.message()).isEqualTo(EnrollmentResult.WAITLIST.getMessage());
        assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CANCELLED);

        ArgumentCaptor<Waitlist> captor = ArgumentCaptor.forClass(Waitlist.class);
        verify(waitlistRepository).save(captor.capture());

        Waitlist savedWaitlist = captor.getValue();
        assertThat(savedWaitlist.getUser()).isEqualTo(student);
        assertThat(savedWaitlist.getCourse()).isEqualTo(course);
        assertThat(savedWaitlist.getWaitlistStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(savedWaitlist.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("수강 확정 실패로 대기열 등록 실패 - 이미 WAITING 상태면 예외가 발생한다")
    void registerByConfirmFailure_fail_already_waiting() {
        // Given
        Enrollment enrollment = createPendingEnrollment(100L, student, course);

        when(waitlistRepository.existsByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.WAITING
        )).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> waitListService.registerByConfirmFailure(student, course, enrollment))
                .isInstanceOf(BadRequestException.class);

        assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.PENDING);

        verify(waitlistRepository, never()).save(any(Waitlist.class));
    }

    @Test
    @DisplayName("다음 대기자 승격 성공 - WAITING 대기자가 있으면 수강 신청을 생성하고 대기자를 승격한다")
    void promoteNext_success() {
        // Given
        Waitlist waitlist = createWaitingWaitlist(200L, student, course);

        when(waitlistRepository.findFirstByCourseAndWaitlistStatusOrderByCreatedAtAsc(
                course,
                WaitlistStatus.WAITING
        )).thenReturn(Optional.of(waitlist));

        // When
        waitListService.promoteNext(course);

        // Then
        assertThat(waitlist.getWaitlistStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(waitlist.getPromotedAt()).isNotNull();
        assertThat(waitlist.getExpiresAt()).isNotNull();
        assertThat(waitlist.getExpiresAt()).isAfter(LocalDateTime.now());

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());

        Enrollment savedEnrollment = captor.getValue();
        assertThat(savedEnrollment.getUser()).isEqualTo(student);
        assertThat(savedEnrollment.getCourse()).isEqualTo(course);
        assertThat(savedEnrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    @DisplayName("다음 대기자 승격 - WAITING 대기자가 없으면 아무 작업도 하지 않는다")
    void promoteNext_no_waiting_user() {
        // Given
        when(waitlistRepository.findFirstByCourseAndWaitlistStatusOrderByCreatedAtAsc(
                course,
                WaitlistStatus.WAITING
        )).thenReturn(Optional.empty());

        // When
        waitListService.promoteNext(course);

        // Then
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("만료된 승격 대기열 처리 성공 - PROMOTED 상태를 EXPIRED로 바꾸고 다음 대기자를 승격한다")
    void expirePromotedWaitlist_success() {
        // Given
        Waitlist expiredWaitlist = createPromotedWaitlist(200L, student, course);

        User nextStudent = createUser(3L, "다음 학생", Role.STUDENT);
        Waitlist nextWaitlist = createWaitingWaitlist(201L, nextStudent, course);

        when(waitlistRepository.findAllByWaitlistStatusAndExpiresAtBefore(
                eq(WaitlistStatus.PROMOTED),
                any(LocalDateTime.class)
        )).thenReturn(List.of(expiredWaitlist));

        when(waitlistRepository.findFirstByCourseAndWaitlistStatusOrderByCreatedAtAsc(
                course,
                WaitlistStatus.WAITING
        )).thenReturn(Optional.of(nextWaitlist));

        // When
        waitListService.expirePromotedWaitlist();

        // Then
        assertThat(expiredWaitlist.getWaitlistStatus()).isEqualTo(WaitlistStatus.EXPIRED);
        assertThat(nextWaitlist.getWaitlistStatus()).isEqualTo(WaitlistStatus.PROMOTED);

        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("만료된 승격 대기열 처리 - 만료 대상이 없으면 아무 작업도 하지 않는다")
    void expirePromotedWaitlist_empty() {
        // Given
        when(waitlistRepository.findAllByWaitlistStatusAndExpiresAtBefore(
                eq(WaitlistStatus.PROMOTED),
                any(LocalDateTime.class)
        )).thenReturn(List.of());

        // When
        waitListService.expirePromotedWaitlist();

        // Then
        verify(waitlistRepository, never())
                .findFirstByCourseAndWaitlistStatusOrderByCreatedAtAsc(any(Course.class), any(WaitlistStatus.class));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("승격 대상자 여부 확인 - PROMOTED 상태면 true를 반환한다")
    void isPromotedUser_true() {
        // Given
        when(waitlistRepository.existsByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.PROMOTED
        )).thenReturn(true);

        // When
        boolean result = waitListService.isPromotedUser(student, course);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("승격 대상자 여부 확인 - PROMOTED 상태가 아니면 false를 반환한다")
    void isPromotedUser_false() {
        // Given
        when(waitlistRepository.existsByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.PROMOTED
        )).thenReturn(false);

        // When
        boolean result = waitListService.isPromotedUser(student, course);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("승격 완료 처리 성공 - PROMOTED 대기열이 있으면 COMPLETED로 변경한다")
    void completeIfPromoted_success() {
        // Given
        Waitlist waitlist = createPromotedWaitlist(200L, student, course);

        when(waitlistRepository.findByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.PROMOTED
        )).thenReturn(Optional.of(waitlist));

        // When
        waitListService.completeIfPromoted(student, course);

        // Then
        assertThat(waitlist.getWaitlistStatus()).isEqualTo(WaitlistStatus.COMPLETED);
    }

    @Test
    @DisplayName("승격 완료 처리 - PROMOTED 대기열이 없으면 아무 작업도 하지 않는다")
    void completeIfPromoted_empty() {
        // Given
        when(waitlistRepository.findByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.PROMOTED
        )).thenReturn(Optional.empty());

        // When
        waitListService.completeIfPromoted(student, course);

        // Then
        verify(waitlistRepository).findByUserAndCourseAndWaitlistStatus(
                student,
                course,
                WaitlistStatus.PROMOTED
        );
    }

    @Test
    @DisplayName("대기자 존재 여부 확인 - WAITING 대기자가 있으면 true를 반환한다")
    void hasWaitingUser_true() {
        // Given
        when(waitlistRepository.existsByCourseAndWaitlistStatus(
                course,
                WaitlistStatus.WAITING
        )).thenReturn(true);

        // When
        boolean result = waitListService.hasWaitingUser(course);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("대기자 존재 여부 확인 - WAITING 대기자가 없으면 false를 반환한다")
    void hasWaitingUser_false() {
        // Given
        when(waitlistRepository.existsByCourseAndWaitlistStatus(
                course,
                WaitlistStatus.WAITING
        )).thenReturn(false);

        // When
        boolean result = waitListService.hasWaitingUser(course);

        // Then
        assertThat(result).isFalse();
    }

    private User createUser(Long id, String name, Role role) {
        User user = User.create(name, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Enrollment createPendingEnrollment(Long id, User user, Course course) {
        Enrollment enrollment = Enrollment.create(user, course);
        ReflectionTestUtils.setField(enrollment, "id", id);
        return enrollment;
    }

    private Waitlist createWaitingWaitlist(Long id, User user, Course course) {
        Waitlist waitlist = Waitlist.create(user, course);
        ReflectionTestUtils.setField(waitlist, "id", id);
        return waitlist;
    }

    private Waitlist createPromotedWaitlist(Long id, User user, Course course) {
        Waitlist waitlist = Waitlist.create(user, course);
        ReflectionTestUtils.setField(waitlist, "id", id);
        waitlist.promote(LocalDateTime.now().minusMinutes(1));
        return waitlist;
    }

    private Course createOpenCourse(Long id, User creator) {
        Course course = Course.create(
                "테스트 강의",
                "테스트 설명",
                10000,
                10,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                creator
        );

        ReflectionTestUtils.setField(course, "id", id);
        course.open();

        return course;
    }
}

