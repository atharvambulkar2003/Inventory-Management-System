package com.ims.exception;

public class UsernameNotExistsException extends RuntimeException{
	public UsernameNotExistsException(String message) {
		super(message);
	}
}
