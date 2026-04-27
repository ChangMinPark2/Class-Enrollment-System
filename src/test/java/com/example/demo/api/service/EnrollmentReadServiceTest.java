package com.example.demo.api.service;

import com.example.demo.api.dto.course.CourseStudentReadAllDto;
import com.example.demo.api.dto.enrollment.EnrollmentReadAllDto;
import com.example.demo.error.exception.BadRequestException;
import com.example.demo.error.exception.NotFoundException;
import com.example.demo.api.persistence.entity.*;
import com.example.demo.api.persistence.repository.CourseRepository;
import com.example.demo.api.persistence.repository.EnrollmentRepository;
import com.example.demo.api.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentReadServiceTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EnrollmentReadService enrollmentReadService;

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
    @DisplayName("내 수강 신청 목록 조회 성공")
    void readAllByUser_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        Enrollment enrollment1 = createConfirmedEnrollment(100L, student, course);
        Enrollment enrollment2 = createPendingEnrollment(101L, student, course);

        Page<Enrollment> page = new PageImpl<>(
                List.of(enrollment1, enrollment2),
                pageable,
                2
        );

        when(userRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findAllByUser(student, pageable))
                .thenReturn(page);

        // When
        EnrollmentReadAllDto response =
                enrollmentReadService.readAllByUser(student.getId(), pageable);

        // Then
        assertThat(response.enrollments()).hasSize(2);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);

        assertThat(response.enrollments().get(0).enrollmentId()).isEqualTo(100L);
        assertThat(response.enrollments().get(0).courseId()).isEqualTo(course.getId());
        assertThat(response.enrollments().get(0).status()).isEqualTo(EnrollmentStatus.CONFIRMED);

        verify(enrollmentRepository).findAllByUser(student, pageable);
    }

    @Test
    @DisplayName("내 수강 신청 목록 조회 실패 - 유저가 존재하지 않으면 예외가 발생한다")
    void readAllByUser_fail_user_not_found() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentReadService.readAllByUser(999L, pageable))
                .isInstanceOf(NotFoundException.class);

        verify(enrollmentRepository, never()).findAllByUser(any(User.class), any(Pageable.class));
    }
    
    private User createUser(Long id, String name, Role role) {
        User user = User.create(name, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
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

    private Enrollment createPendingEnrollment(Long id, User user, Course course) {
        Enrollment enrollment = Enrollment.create(user, course);
        ReflectionTestUtils.setField(enrollment, "id", id);
        return enrollment;
    }

    private Enrollment createConfirmedEnrollment(Long id, User user, Course course) {
        Enrollment enrollment = Enrollment.create(user, course);
        ReflectionTestUtils.setField(enrollment, "id", id);
        enrollment.confirm();
        return enrollment;
    }
}
