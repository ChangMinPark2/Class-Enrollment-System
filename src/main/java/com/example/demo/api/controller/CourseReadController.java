package com.example.demo.api.controller;

import com.example.demo.api.dto.course.CourseReadAllDto;
import com.example.demo.api.dto.course.CourseReadDetailDto;
import com.example.demo.api.persistence.entity.CourseStatus;
import com.example.demo.api.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseReadController {
    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<CourseReadAllDto> readAll(
            @RequestParam(required = false) CourseStatus status
    ) {
        return ResponseEntity.ok(courseService.readAll(status));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseReadDetailDto> read(
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(courseService.read(courseId));
    }
}
