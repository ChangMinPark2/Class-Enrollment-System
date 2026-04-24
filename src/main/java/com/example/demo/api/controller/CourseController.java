package com.example.demo.api.controller;

import com.example.demo.api.dto.course.CourseCreateDto;
import com.example.demo.api.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class CourseController {
    private final CourseService courseService;

    @PostMapping("/{userId}/courses")
    public ResponseEntity<String> createCourse(
            @PathVariable Long userId,
            @RequestBody @Valid CourseCreateDto dto
    ) {
        courseService.create(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("강의가 생성되었습니다.");
    }

    @PostMapping("/{userId}/courses/{courseId}/open")
    public ResponseEntity<String> openCourse(
            @PathVariable Long userId,
            @PathVariable Long courseId
    ) {
        courseService.openCourse(userId, courseId);
        return ResponseEntity.ok("강의가 오픈되었습니다.");
    }
}
