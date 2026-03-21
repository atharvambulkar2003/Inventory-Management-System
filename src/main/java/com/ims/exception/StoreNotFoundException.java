package com.ims.exception;

public class StoreNotFoundException extends RuntimeException{
	public StoreNotFoundException(String message) {
		super(message);
	}
}
