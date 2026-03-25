package com.ims.dto;

import lombok.Data;

@Data
public class SigninResponseVO {
	private String token;
	private String fullName;
	private String email;
	private String role;
	private boolean active;
	private String storeName;
	private String gstNumber;
}
