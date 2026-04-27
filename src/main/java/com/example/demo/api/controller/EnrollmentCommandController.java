package com.example.demo.api.controller;

import com.example.demo.api.dto.enrollment.EnrollmentCreateDto;
import com.example.demo.api.dto.enrollment.EnrollmentCreateResponseDto;
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

    //    @PostMapping
//    public ResponseEntity<String> create(@RequestBody @Valid EnrollmentCreateDto dto) {
//        enrollmentService.create(dto);
//        return ResponseEntity.status(HttpStatus.CREATED).body("수강 신청이 완료되었습니다.");
//    }
    @PostMapping
    public ResponseEntity<EnrollmentCreateResponseDto> create(
            @RequestBody @Valid EnrollmentCreateDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.create(dto));
    }

    @PostMapping("/{enrollmentId}/confirm")
    public ResponseEntity<String> confirm(
            @RequestParam Long userId,
            @PathVariable Long enrollmentId
    ) {
        enrollmentService.confirm(userId, enrollmentId);
        return ResponseEntity.ok("결제가 완료되었습니다.");
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
