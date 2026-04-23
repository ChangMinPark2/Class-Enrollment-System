package com.example.demo.error.exception;


import com.example.demo.error.model.ErrorCode;

public class EnrollmentException extends RuntimeException {
	private ErrorCode errorCode;

	public EnrollmentException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
