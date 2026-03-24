package com.ims.dto;

import lombok.Data;

@Data
public class ForgetPasswordChangePasswordByEmailDto {
	private String otp;
	private String email;
	private String password;
}
