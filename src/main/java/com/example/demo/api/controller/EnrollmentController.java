package com.example.demo.api.controller;

import com.example.demo.api.dto.EnrollmentCreateDto;
import com.example.demo.api.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<String> create(@RequestBody @Valid EnrollmentCreateDto dto) {
        enrollmentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("수강 신청이 완료되었습니다.");
    }
}
