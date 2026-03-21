package com.ims.exception;

public class ProductNameAlreadyExistsInStore extends RuntimeException{
	public ProductNameAlreadyExistsInStore(String message) {
		super(message);
	}
}
