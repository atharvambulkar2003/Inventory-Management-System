package com.ims.dto;

import lombok.Data;

@Data
public class UserSignupDto {
	private String username;
	private String fullName;
    private String password; 
    private String email;
    private String storeName;
    private String gstNumber;
}
	