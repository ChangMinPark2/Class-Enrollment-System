package com.example.demo.api.controller;

import com.example.demo.api.dto.enrollment.EnrollmentReadAllDto;
import com.example.demo.api.service.EnrollmentReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class EnrollmentReadController {
    private final EnrollmentReadService enrollmentReadService;

    @GetMapping("/{userId}/enrollments")
    public ResponseEntity<EnrollmentReadAllDto> readAllByUser(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(enrollmentReadService.readAllByUser(userId, pageable));
    }
}
