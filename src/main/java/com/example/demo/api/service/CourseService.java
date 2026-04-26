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
import com.example.demo.error.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public void create(Long userId, CourseCreateDto dto) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));

        validateCreator(user);
        validateDate(dto);

        final Course course = Course.create(
                dto.title(),
                dto.description(),
                dto.price(),
                dto.maxCapacity(),
                dto.startedAt(),
                dto.endedAt(),
                user
        );

        courseRepository.save(course);
    }

    public void open(Long userId, Long courseId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));

        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_COURSE));

        validateCourseOwner(user, course);
        validateOpenStatus(course);
        validateCourseNotEnded(course);

        course.open();
    }

    public void close(Long userId, Long courseId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));

        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_COURSE));

        validateCourseOwner(user, course);
        validateCloseStatus(course);

        course.close();
    }

    private void validateCloseStatus(Course course) {
        if (course.getCourseStatus() != CourseStatus.OPEN) {
            throw new BadRequestException(ErrorCode.INVALID_COURSE_CLOSE_STATUS);
        }
    }

    @Transactional(readOnly = true)
    public CourseReadAllDto readAll(CourseStatus status) {
        final List<Course> courses = Optional.ofNullable(status)
                .map(courseRepository::findAllByCourseStatus)
                .orElseGet(courseRepository::findAll);

        return new CourseReadAllDto(
                courses.stream()
                        .map(Course::toSummaryDto)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public CourseReadDetailDto read(Long courseId) {
        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_COURSE));

        return course.toDetailDto();
    }

    private void validateCreator(User user) {
        if (user.getRole() != Role.CREATOR) {
            throw new BadRequestException(ErrorCode.INVALID_ROLE);
        }
    }

    private void validateDate(CourseCreateDto dto) {
        if (dto.startedAt().isAfter(dto.endedAt())) {
            throw new BadRequestException(ErrorCode.INVALID_DATE_RANGE);
        }
    }


    private void validateCourseOwner(User user, Course course) {
        if (!course.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(ErrorCode.INVALID_COURSE_OWNER);
        }
    }

    private void validateOpenStatus(Course course) {
        if (course.getCourseStatus() != CourseStatus.DRAFT) {
            throw new BadRequestException(ErrorCode.INVALID_COURSE_STATUS);
        }
    }

    private void validateCourseNotEnded(Course course) {
        if (course.getEndedAt().isBefore(LocalDate.now())) {
            throw new BadRequestException(ErrorCode.INVALID_COURSE_PERIOD);
        }
    }
}
