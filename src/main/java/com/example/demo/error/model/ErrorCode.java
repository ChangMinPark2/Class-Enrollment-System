package com.example.demo.error.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ErrorCode {
    //BadRequest 400 error
    DUPLICATE_USER_NAME("중복된 이름이 존재합니다.", HttpStatus.BAD_REQUEST);

    private String message;
    private HttpStatus statusCode;
}
