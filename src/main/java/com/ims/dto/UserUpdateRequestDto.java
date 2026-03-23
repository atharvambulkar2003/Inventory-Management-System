package com.ims.dto;

import lombok.Data;

@Data
public class UserUpdateRequestDto {
	private String otp;
	private String fullName;
	private String storeName;
}
