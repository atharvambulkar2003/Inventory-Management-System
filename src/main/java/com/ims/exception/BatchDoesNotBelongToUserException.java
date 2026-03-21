package com.ims.exception;

public class BatchDoesNotBelongToUserException extends RuntimeException{
	public BatchDoesNotBelongToUserException(String message) {
		super(message);
	}
}
