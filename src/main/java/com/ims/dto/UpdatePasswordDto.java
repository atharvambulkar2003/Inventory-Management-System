package com.ims.dto;

import lombok.Data;

@Data
public class UpdatePasswordDto {
	private String otp;
	private String username;
	private String password;
	private String newPassword;
}
