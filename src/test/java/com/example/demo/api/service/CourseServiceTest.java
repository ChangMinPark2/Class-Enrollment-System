package com.example.demo.api.service;

import com.example.demo.api.dto.course.CourseCreateDto;
import com.example.demo.api.dto.course.CourseReadAllDto;
import com.example.demo.api.dto.course.CourseReadDetailDto;
import com.example.demo.api.persistence.entity.Course;
import com.example.demo.api.persistence.entity.CourseStatus;
import com.example.demo.api.persistence.entity.Role;
import com.example.demo.api.persistence.entity.User;
import com.example.demo.api.persistence.repository.CourseRepository;
import com.example.demo.api.persistence.repository.UserRepository;
import com.example.demo.error.exception.BadRequestException;
import com.example.demo.error.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    @DisplayName("강사는 강의를 생성할 수 있다")
    void create_success() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);

        CourseCreateDto dto = new CourseCreateDto(
                "스프링 강의",
                "스프링 강의 설명",
                10000,
                30,
                LocalDate.now(),
                LocalDate.now().plusDays(10)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        // WHEN
        courseService.create(1L, dto);

        // THEN
        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository, times(1)).save(courseCaptor.capture());

        Course savedCourse = courseCaptor.getValue();

        assertThat(savedCourse.getTitle()).isEqualTo("스프링 강의");
        assertThat(savedCourse.getDescription()).isEqualTo("스프링 강의 설명");
        assertThat(savedCourse.getPrice()).isEqualTo(10000);
        assertThat(savedCourse.getMaxCapacity()).isEqualTo(30);
        assertThat(savedCourse.getCurrentCapacity()).isZero();
        assertThat(savedCourse.getCourseStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(savedCourse.getStartedAt()).isEqualTo(dto.startedAt());
        assertThat(savedCourse.getEndedAt()).isEqualTo(dto.endedAt());
        assertThat(savedCourse.getUser()).isEqualTo(creator);
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 강의 생성을 요청하면 예외가 발생한다")
    void create_fail_userNotFound() {
        // GIVEN
        CourseCreateDto dto = createCourseDto(
                LocalDate.now(),
                LocalDate.now().plusDays(10)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> courseService.create(1L, dto))
                .isInstanceOf(NotFoundException.class);

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("강사가 아닌 사용자가 강의 생성을 요청하면 예외가 발생한다")
    void create_fail_invalidRole() {
        // GIVEN
        User student = createUser(1L, "학생", Role.STUDENT);
        CourseCreateDto dto = createCourseDto(
                LocalDate.now(),
                LocalDate.now().plusDays(10)
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(student));

        // WHEN & THEN
        assertThatThrownBy(() -> courseService.create(1L, dto))
                .isInstanceOf(BadRequestException.class);

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 강의 생성 시 예외가 발생한다")
    void create_fail_invalidDateRange() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        CourseCreateDto dto = createCourseDto(
                LocalDate.now().plusDays(10),
                LocalDate.now()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        // WHEN & THEN
        assertThatThrownBy(() -> courseService.create(1L, dto))
                .isInstanceOf(BadRequestException.class);

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("강의 생성자는 DRAFT 상태의 강의를 OPEN 상태로 변경할 수 있다")
    void open_success() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        Course course = createCourse(10L, creator, LocalDate.now().plusDays(10));

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // WHEN
        courseService.open(1L, 10L);

        // THEN
        assertThat(course.getCourseStatus()).isEqualTo(CourseStatus.OPEN);
    }

    @Test
    @DisplayName("강의 소유자가 아니면 강의를 OPEN 할 수 없다")
    void open_fail_invalidOwner() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        User otherUser = createUser(2L, "다른 사용자", Role.CREATOR);
        Course course = createCourse(10L, creator, LocalDate.now().plusDays(10));

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // WHEN & THEN
        assertThatThrownBy(() -> courseService.open(2L, 10L))
                .isInstanceOf(BadRequestException.class);

        assertThat(course.getCourseStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("DRAFT 상태가 아닌 강의는 OPEN 할 수 없다")
    void open_fail_invalidStatus() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        Course course = createCourse(10L, creator, LocalDate.now().plusDays(10));
        course.open();

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // WHEN & THEN
        assertThatThrownBy(() -> courseService.open(1L, 10L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("종료일이 지난 강의는 OPEN 할 수 없다")
    void open_fail_courseEnded() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        Course course = createCourse(10L, creator, LocalDate.now().minusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // WHEN & THEN
        assertThatThrownBy(() -> courseService.open(1L, 10L))
                .isInstanceOf(BadRequestException.class);

        assertThat(course.getCourseStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("강의 생성자는 OPEN 상태의 강의를 CLOSED 상태로 변경할 수 있다")
    void close_success() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        Course course = createCourse(10L, creator, LocalDate.now().plusDays(10));
        course.open();

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // WHEN
        courseService.close(1L, 10L);

        // THEN
        assertThat(course.getCourseStatus()).isEqualTo(CourseStatus.CLOSED);
    }

    @Test
    @DisplayName("OPEN 상태가 아닌 강의는 CLOSED 상태로 변경할 수 없다")
    void close_fail_invalidStatus() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        Course course = createCourse(10L, creator, LocalDate.now().plusDays(10));

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // WHEN & THEN
        assertThatThrownBy(() -> courseService.close(1L, 10L))
                .isInstanceOf(BadRequestException.class);

        assertThat(course.getCourseStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("상태 필터가 없으면 전체 강의 목록을 조회한다")
    void readAll_withoutStatus_success() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        Course course1 = createCourse(10L, creator, LocalDate.now().plusDays(10));
        Course course2 = createCourse(11L, creator, LocalDate.now().plusDays(20));

        when(courseRepository.findAll()).thenReturn(List.of(course1, course2));

        // WHEN
        CourseReadAllDto result = courseService.readAll(null);

        // THEN
        assertThat(result.courses()).hasSize(2);
        verify(courseRepository, times(1)).findAll();
        verify(courseRepository, never()).findAllByCourseStatus(any());
    }

    @Test
    @DisplayName("상태 필터가 있으면 해당 상태의 강의 목록만 조회한다")
    void readAll_withStatus_success() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        Course course = createCourse(10L, creator, LocalDate.now().plusDays(10));
        course.open();

        when(courseRepository.findAllByCourseStatus(CourseStatus.OPEN))
                .thenReturn(List.of(course));

        // WHEN
        CourseReadAllDto result = courseService.readAll(CourseStatus.OPEN);

        // THEN
        assertThat(result.courses()).hasSize(1);
        assertThat(result.courses().get(0).status()).isEqualTo(CourseStatus.OPEN);

        verify(courseRepository, times(1)).findAllByCourseStatus(CourseStatus.OPEN);
        verify(courseRepository, never()).findAll();
    }

    @Test
    @DisplayName("courseId로 강의 상세 정보를 조회한다")
    void read_success() {
        // GIVEN
        User creator = createUser(1L, "강사", Role.CREATOR);
        Course course = createCourse(10L, creator, LocalDate.now().plusDays(10));

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));

        // WHEN
        CourseReadDetailDto result = courseService.read(10L);

        // THEN
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("스프링 강의");
        assertThat(result.currentCapacity()).isZero();
        assertThat(result.status()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("존재하지 않는 강의 상세 조회 시 예외가 발생한다")
    void read_fail_courseNotFound() {
        // GIVEN
        when(courseRepository.findById(10L)).thenReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> courseService.read(10L))
                .isInstanceOf(NotFoundException.class);
    }

    private User createUser(Long id, String name, Role role) {
        User user = User.create(name, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Course createCourse(Long id, User user, LocalDate endedAt) {
        Course course = Course.create(
                "스프링 강의",
                "스프링 강의 설명",
                10000,
                30,
                LocalDate.now(),
                endedAt,
                user
        );

        ReflectionTestUtils.setField(course, "id", id);
        return course;
    }


    private CourseCreateDto createCourseDto(LocalDate startedAt, LocalDate endedAt) {
        return new CourseCreateDto(
                "스프링 강의",
                "스프링 강의 설명",
                10000,
                30,
                startedAt,
                endedAt
        );
    }
}
