package com.example.demo.api.controller;

import com.example.demo.api.dto.enrollment.EnrollmentCreateDto;
import com.example.demo.api.dto.enrollment.EnrollmentResponseDto;
import com.example.demo.api.service.EnrollmentCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentCommandController {
    private final EnrollmentCommandService enrollmentService;

    @PostMapping
    public ResponseEntity<EnrollmentResponseDto> create(
            @RequestBody @Valid EnrollmentCreateDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.create(dto));
    }

    @PostMapping("/{enrollmentId}/confirm")
    public ResponseEntity<EnrollmentResponseDto> confirm(
            @RequestParam Long userId,
            @PathVariable Long enrollmentId
    ) {
        return ResponseEntity.ok(enrollmentService.confirm(userId, enrollmentId));
    }

    @PostMapping("/{enrollmentId}/cancel")
    public ResponseEntity<String> cancel(
            @RequestParam Long userId,
            @PathVariable Long enrollmentId
    ) {
        enrollmentService.cancel(userId, enrollmentId);
        return ResponseEntity.ok("수강 신청이 취소되었습니다.");
    }
}
