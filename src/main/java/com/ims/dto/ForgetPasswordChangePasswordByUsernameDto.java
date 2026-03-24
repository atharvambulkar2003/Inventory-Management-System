package com.ims.dto;

import lombok.Data;

@Data
public class ForgetPasswordChangePasswordByUsernameDto {
	private String otp;
	private String username;
	private String password;
}
