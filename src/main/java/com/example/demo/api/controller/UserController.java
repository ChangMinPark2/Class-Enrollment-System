package com.example.demo.api.controller;

import com.example.demo.api.dto.UserCreateDto;
import com.example.demo.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<String> create(@Valid @RequestBody UserCreateDto dto) {
        userService.signUp(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("예약이 성공적으로 생성되었습니다.");
    }
}
