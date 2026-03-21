package com.ims.exception;


public class ProductCodeAlreadyExistsInStoreException extends RuntimeException{
	public ProductCodeAlreadyExistsInStoreException(String message) {
		super(message);
	}
}
