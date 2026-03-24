package com.ims.exception;

public class EmailFailedException extends RuntimeException{
	public EmailFailedException(String message) {
		super(message);
	}
}
