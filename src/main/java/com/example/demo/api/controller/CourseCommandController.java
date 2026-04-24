package com.example.demo.api.controller;

import com.example.demo.api.dto.course.CourseReadAllDto;
import com.example.demo.api.persistence.entity.CourseStatus;
import com.example.demo.api.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course")
public class CourseCommandController {
    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<CourseReadAllDto> readAllCourses(
            @RequestParam(required = false) CourseStatus status
    ) {
        return ResponseEntity.ok(courseService.readAllCourse(status));
    }
}
