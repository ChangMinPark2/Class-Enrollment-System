package com.example.demo.error.exception;


import com.example.demo.error.model.ErrorCode;

public class NotFoundException extends EnrollmentException {
	public NotFoundException(ErrorCode errorCode) {
		super(errorCode);
	}
}
