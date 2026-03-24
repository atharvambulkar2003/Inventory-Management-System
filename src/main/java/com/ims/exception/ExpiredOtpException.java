package com.ims.exception;

public class ExpiredOtpException extends RuntimeException {
	public ExpiredOtpException(String message) {
		super(message);
	}
}
