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

    private User createUser(Long id, String name, Role role) {
        User user = User.create(name, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
