package com.example.demo.error.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ErrorCode {
    //NotFound 404 error
    FAIL_NOT_USER("해당 유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // BadRequest 400
    INVALID_ROLE("강의 생성은 강사만 가능합니다.", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE("시작일은 종료일보다 이후일 수 없습니다.", HttpStatus.BAD_REQUEST);

    private String message;
    private HttpStatus statusCode;
}
