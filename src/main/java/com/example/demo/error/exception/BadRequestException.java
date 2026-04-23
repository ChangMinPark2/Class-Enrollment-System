package com.example.demo.error.exception;


import com.example.demo.error.model.ErrorCode;

public class BadRequestException extends EnrollmentException {
    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }
}
