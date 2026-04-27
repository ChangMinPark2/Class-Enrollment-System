package com.example.demo.api.service;

import com.example.demo.api.dto.enrollment.EnrollmentCreateDto;
import com.example.demo.api.dto.enrollment.EnrollmentResponseDto;
import com.example.demo.api.dto.enrollment.EnrollmentResult;
import com.example.demo.api.persistence.entity.*;
import com.example.demo.api.persistence.repository.CourseRepository;
import com.example.demo.api.persistence.repository.EnrollmentRepository;
import com.example.demo.api.persistence.repository.UserRepository;
import com.example.demo.error.exception.BadRequestException;
import com.example.demo.error.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EnrollmentCommandServiceTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private WaitListService waitlistService;

    @InjectMocks
    private EnrollmentCommandService enrollmentCommandService;

    private User creator;
    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        creator = createUser(1L, "강사", Role.CREATOR);
        student = createUser(2L, "학생", Role.STUDENT);
        course = createOpenCourse(10L, creator, 10, 0);
    }

    @Test
    @DisplayName("수강 신청 성공 - 정원이 남아있고 대기자가 없으면 수강 신청된다")
    void create_success_enrollment() {
        // Given
        EnrollmentCreateDto dto = new EnrollmentCreateDto(student.getId(), course.getId());

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId()))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserAndCourse(student, course))
                .thenReturn(false);
        when(waitlistService.hasWaitingUser(course))
                .thenReturn(false);

        // When
        EnrollmentResponseDto response = enrollmentCommandService.create(dto);

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.ENROLLMENT);
        assertThat(response.message()).isEqualTo(EnrollmentResult.ENROLLMENT.getMessage());

        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(waitlistService, never()).register(any(User.class), any(Course.class));
    }

    @Test
    @DisplayName("수강 신청 실패 - 유저가 존재하지 않으면 예외가 발생한다")
    void create_fail_user_not_found() {
        // Given
        EnrollmentCreateDto dto = new EnrollmentCreateDto(999L, course.getId());

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.create(dto))
                .isInstanceOf(NotFoundException.class);

        verify(courseRepository, never()).findById(anyLong());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 - 강의가 존재하지 않으면 예외가 발생한다")
    void create_fail_course_not_found() {
        // Given
        EnrollmentCreateDto dto = new EnrollmentCreateDto(student.getId(), 999L);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.create(dto))
                .isInstanceOf(NotFoundException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 - 강의 상태가 OPEN이 아니면 예외가 발생한다")
    void create_fail_course_not_open() {
        // Given
        Course draftCourse = createDraftCourse(10L, creator);

        EnrollmentCreateDto dto = new EnrollmentCreateDto(student.getId(), draftCourse.getId());

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(draftCourse.getId()))
                .thenReturn(Optional.of(draftCourse));

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.create(dto))
                .isInstanceOf(BadRequestException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 - 이미 수강 신청한 강의면 예외가 발생한다")
    void create_fail_already_enrolled() {
        // Given
        EnrollmentCreateDto dto = new EnrollmentCreateDto(student.getId(), course.getId());

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId()))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserAndCourse(student, course))
                .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.create(dto))
                .isInstanceOf(BadRequestException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 실패 - 강의 생성자는 본인 강의를 신청할 수 없다")
    void create_fail_self_enrollment() {
        // Given
        EnrollmentCreateDto dto = new EnrollmentCreateDto(creator.getId(), course.getId());

        when(userRepository.findById(creator.getId()))
                .thenReturn(Optional.of(creator));
        when(courseRepository.findById(course.getId()))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserAndCourse(creator, course))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.create(dto))
                .isInstanceOf(BadRequestException.class);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 - 정원이 가득 차면 대기열에 등록한다")
    void create_waitlist_when_course_full() {
        // Given
        Course fullCourse = createOpenCourse(10L, creator, 10, 10);

        EnrollmentCreateDto dto = new EnrollmentCreateDto(student.getId(), fullCourse.getId());

        EnrollmentResponseDto waitListResponse = new EnrollmentResponseDto(
                EnrollmentResult.WAITLIST,
                EnrollmentResult.WAITLIST.getMessage()
        );

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(fullCourse.getId()))
                .thenReturn(Optional.of(fullCourse));
        when(enrollmentRepository.existsByUserAndCourse(student, fullCourse))
                .thenReturn(false);
        when(waitlistService.register(student, fullCourse))
                .thenReturn(waitListResponse);

        // When
        EnrollmentResponseDto response = enrollmentCommandService.create(dto);

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.WAITLIST);
        assertThat(response.message()).isEqualTo(EnrollmentResult.WAITLIST.getMessage());

        verify(waitlistService).register(student, fullCourse);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 신청 - 정원이 남아있어도 대기자가 있으면 대기열에 등록한다")
    void create_waitlist_when_waiting_user_exists() {
        // Given
        EnrollmentCreateDto dto = new EnrollmentCreateDto(student.getId(), course.getId());

        EnrollmentResponseDto waitListResponse = new EnrollmentResponseDto(
                EnrollmentResult.WAITLIST,
                EnrollmentResult.WAITLIST.getMessage()
        );

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(course.getId()))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserAndCourse(student, course))
                .thenReturn(false);
        when(waitlistService.hasWaitingUser(course))
                .thenReturn(true);
        when(waitlistService.register(student, course))
                .thenReturn(waitListResponse);

        // When
        EnrollmentResponseDto response = enrollmentCommandService.create(dto);

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.WAITLIST);

        verify(waitlistService).register(student, course);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("수강 확정 성공 - 대기자가 없고 정원이 남아있으면 수강 확정된다")
    void confirm_success() {
        // Given
        Enrollment enrollment = createPendingEnrollment(100L, student, course);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(waitlistService.hasWaitingUser(course))
                .thenReturn(false);
        when(courseRepository.increaseCapacityIfAvailable(course.getId()))
                .thenReturn(1);

        // When
        EnrollmentResponseDto response =
                enrollmentCommandService.confirm(student.getId(), enrollment.getId());

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.CONFIRMED);
        assertThat(response.message()).isEqualTo(EnrollmentResult.CONFIRMED.getMessage());
        assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(enrollment.getConfirmedAt()).isNotNull();

        verify(waitlistService).completeIfPromoted(student, course);
        verify(waitlistService, never())
                .registerByConfirmFailure(any(User.class), any(Course.class), any(Enrollment.class));
    }

    @Test
    @DisplayName("수강 확정 실패 - 유저가 존재하지 않으면 예외가 발생한다")
    void confirm_fail_user_not_found() {
        // Given
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.confirm(999L, 100L))
                .isInstanceOf(NotFoundException.class);

        verify(enrollmentRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("수강 확정 실패 - 수강 신청 내역이 존재하지 않으면 예외가 발생한다")
    void confirm_fail_enrollment_not_found() {
        // Given
        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.confirm(student.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("수강 확정 실패 - 본인의 수강 신청 내역이 아니면 예외가 발생한다")
    void confirm_fail_invalid_owner() {
        // Given
        User otherUser = createUser(3L, "다른 학생", Role.STUDENT);
        Enrollment enrollment = createPendingEnrollment(100L, otherUser, course);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.confirm(student.getId(), enrollment.getId()))
                .isInstanceOf(BadRequestException.class);

        verify(courseRepository, never()).increaseCapacityIfAvailable(anyLong());
    }

    @Test
    @DisplayName("수강 확정 실패 - PENDING 상태가 아니면 예외가 발생한다")
    void confirm_fail_not_pending() {
        // Given
        Enrollment enrollment = createPendingEnrollment(100L, student, course);
        enrollment.confirm();

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.confirm(student.getId(), enrollment.getId()))
                .isInstanceOf(BadRequestException.class);

        verify(courseRepository, never()).increaseCapacityIfAvailable(anyLong());
    }

    @Test
    @DisplayName("수강 확정 실패 - 대기자가 있고 승격 대상자가 아니면 대기열로 이동한다")
    void confirm_register_waitlist_when_waiting_user_exists_and_not_promoted() {
        // Given
        Enrollment enrollment = createPendingEnrollment(100L, student, course);

        EnrollmentResponseDto waitListResponse = new EnrollmentResponseDto(
                EnrollmentResult.WAITLIST,
                EnrollmentResult.WAITLIST.getMessage()
        );

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(waitlistService.hasWaitingUser(course))
                .thenReturn(true);
        when(waitlistService.isPromotedUser(student, course))
                .thenReturn(false);
        when(waitlistService.registerByConfirmFailure(student, course, enrollment))
                .thenReturn(waitListResponse);

        // When
        EnrollmentResponseDto response =
                enrollmentCommandService.confirm(student.getId(), enrollment.getId());

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.WAITLIST);

        verify(courseRepository, never()).increaseCapacityIfAvailable(anyLong());
        verify(waitlistService).registerByConfirmFailure(student, course, enrollment);
    }

    @Test
    @DisplayName("수강 확정 실패 - 정원이 부족하면 대기열로 이동한다")
    void confirm_register_waitlist_when_capacity_not_available() {
        // Given
        Enrollment enrollment = createPendingEnrollment(100L, student, course);

        EnrollmentResponseDto waitListResponse = new EnrollmentResponseDto(
                EnrollmentResult.WAITLIST,
                EnrollmentResult.WAITLIST.getMessage()
        );

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(waitlistService.hasWaitingUser(course))
                .thenReturn(false);
        when(courseRepository.increaseCapacityIfAvailable(course.getId()))
                .thenReturn(0);
        when(waitlistService.registerByConfirmFailure(student, course, enrollment))
                .thenReturn(waitListResponse);

        // When
        EnrollmentResponseDto response =
                enrollmentCommandService.confirm(student.getId(), enrollment.getId());

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.WAITLIST);
        assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.PENDING);

        verify(waitlistService).registerByConfirmFailure(student, course, enrollment);
        verify(waitlistService, never()).completeIfPromoted(any(User.class), any(Course.class));
    }

    @Test
    @DisplayName("수강 확정 성공 - 대기자가 있어도 승격 대상자면 수강 확정된다")
    void confirm_success_when_promoted_user() {
        // Given
        Enrollment enrollment = createPendingEnrollment(100L, student, course);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(waitlistService.hasWaitingUser(course))
                .thenReturn(true);
        when(waitlistService.isPromotedUser(student, course))
                .thenReturn(true);
        when(courseRepository.increaseCapacityIfAvailable(course.getId()))
                .thenReturn(1);

        // When
        EnrollmentResponseDto response =
                enrollmentCommandService.confirm(student.getId(), enrollment.getId());

        // Then
        assertThat(response.type()).isEqualTo(EnrollmentResult.CONFIRMED);
        assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);

        verify(waitlistService).completeIfPromoted(student, course);
    }

    @Test
    @DisplayName("수강 취소 성공 - 확정된 수강 신청이면 취소하고 정원을 감소시킨 뒤 다음 대기자를 승격한다")
    void cancel_success() {
        // Given
        Enrollment enrollment = createConfirmedEnrollment(100L, student, course);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.cancelIfConfirmed(
                enrollment.getId(),
                EnrollmentStatus.CONFIRMED,
                EnrollmentStatus.CANCELLED
        )).thenReturn(1);

        // When
        enrollmentCommandService.cancel(student.getId(), enrollment.getId());

        // Then
        assertThat(course.getCurrentCapacity()).isEqualTo(-1);

        verify(enrollmentRepository).cancelIfConfirmed(
                enrollment.getId(),
                EnrollmentStatus.CONFIRMED,
                EnrollmentStatus.CANCELLED
        );
        verify(waitlistService).promoteNext(course);
    }

    @Test
    @DisplayName("수강 취소 실패 - 유저가 존재하지 않으면 예외가 발생한다")
    void cancel_fail_user_not_found() {
        // Given
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.cancel(999L, 100L))
                .isInstanceOf(NotFoundException.class);

        verify(enrollmentRepository, never()).findById(anyLong());
        verify(waitlistService, never()).promoteNext(any(Course.class));
    }

    @Test
    @DisplayName("수강 취소 실패 - 수강 신청 내역이 존재하지 않으면 예외가 발생한다")
    void cancel_fail_enrollment_not_found() {
        // Given
        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.cancel(student.getId(), 999L))
                .isInstanceOf(NotFoundException.class);

        verify(waitlistService, never()).promoteNext(any(Course.class));
    }

    @Test
    @DisplayName("수강 취소 실패 - 본인의 수강 신청 내역이 아니면 예외가 발생한다")
    void cancel_fail_invalid_owner() {
        // Given
        User otherUser = createUser(3L, "다른 학생", Role.STUDENT);
        Enrollment enrollment = createConfirmedEnrollment(100L, otherUser, course);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.cancel(student.getId(), enrollment.getId()))
                .isInstanceOf(BadRequestException.class);

        verify(enrollmentRepository, never()).cancelIfConfirmed(anyLong(), any(), any());
        verify(waitlistService, never()).promoteNext(any(Course.class));
    }

    @Test
    @DisplayName("수강 취소 실패 - CONFIRMED 상태가 아니면 예외가 발생한다")
    void cancel_fail_not_confirmed() {
        // Given
        Enrollment enrollment = createPendingEnrollment(100L, student, course);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.cancel(student.getId(), enrollment.getId()))
                .isInstanceOf(BadRequestException.class);

        verify(enrollmentRepository, never()).cancelIfConfirmed(anyLong(), any(), any());
        verify(waitlistService, never()).promoteNext(any(Course.class));
    }

    @Test
    @DisplayName("수강 취소 실패 - cancelIfConfirmed 결과가 0이면 예외가 발생한다")
    void cancel_fail_cancel_count_zero() {
        // Given
        Enrollment enrollment = createConfirmedEnrollment(100L, student, course);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.cancelIfConfirmed(
                enrollment.getId(),
                EnrollmentStatus.CONFIRMED,
                EnrollmentStatus.CANCELLED
        )).thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.cancel(student.getId(), enrollment.getId()))
                .isInstanceOf(BadRequestException.class);

        verify(waitlistService, never()).promoteNext(any(Course.class));
    }

    @Test
    @DisplayName("수강 취소 실패 - 취소 원자 업데이트에 실패하면 예외가 발생한다")
    void cancel_fail_when_atomic_cancel_update_failed() {
        // Given
        Enrollment enrollment = createConfirmedEnrollment(100L, student, course);

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findById(enrollment.getId()))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.cancelIfConfirmed(
                enrollment.getId(),
                EnrollmentStatus.CONFIRMED,
                EnrollmentStatus.CANCELLED
        )).thenReturn(0);

        // When & Then
        assertThatThrownBy(() -> enrollmentCommandService.cancel(student.getId(), enrollment.getId()))
                .isInstanceOf(BadRequestException.class);

        verify(waitlistService, never()).promoteNext(any(Course.class));
    }

    private Enrollment createPendingEnrollment(Long id, User user, Course course) {
        Enrollment enrollment = Enrollment.create(user, course);
        ReflectionTestUtils.setField(enrollment, "id", id);
        return enrollment;
    }

    private User createUser(Long id, String name, Role role) {
        User user = User.create(name, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Enrollment createConfirmedEnrollment(Long id, User user, Course course) {
        Enrollment enrollment = Enrollment.create(user, course);
        ReflectionTestUtils.setField(enrollment, "id", id);
        enrollment.confirm();
        return enrollment;
    }

    private Course createOpenCourse(Long id, User creator, int maxCapacity, int currentCapacity) {
        Course course = Course.create(
                "테스트 강의",
                "테스트 설명",
                10000,
                maxCapacity,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                creator
        );

        ReflectionTestUtils.setField(course, "id", id);
        ReflectionTestUtils.setField(course, "currentCapacity", currentCapacity);
        course.open();

        return course;
    }

    private Course createDraftCourse(Long id, User creator) {
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

        return course;
    }
}
