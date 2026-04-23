package com.example.demo.api.service;

import com.example.demo.api.dto.CourseCreateDto;
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

    public void openCourse(Long userId, Long courseId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_USER));

        final Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.FAIL_NOT_COURSE));

        validateCourseOwner(user, course);
        validateOpenStatus(course);
        validateCourseNotEnded(course);

        course.open();
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
