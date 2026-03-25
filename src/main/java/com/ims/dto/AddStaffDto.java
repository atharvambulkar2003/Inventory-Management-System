package com.ims.dto;

import lombok.Data;

@Data
public class AddStaffDto {
	private String username;
	private String fullName;
	private String password; 
	private String email;
}
