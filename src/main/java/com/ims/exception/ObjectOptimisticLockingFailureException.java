package com.ims.exception;

public class ObjectOptimisticLockingFailureException extends RuntimeException {
	public ObjectOptimisticLockingFailureException(String message) {
		super(message);
	}
}
