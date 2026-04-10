package com.ims.service;

import com.ims.dto.ForgetPasswordChangePasswordByEmailDto;
import com.ims.dto.ForgetPasswordChangePasswordByUsernameDto;
import com.ims.dto.SigninResponseVO;
import com.ims.dto.UpdatePasswordDto;
import com.ims.dto.UpdatePasswordOtpDto;
import com.ims.dto.UserSignupDto;
import com.ims.dto.UserUpdateRequestDto;

import jakarta.transaction.Transactional;

public interface UserService {

	String signup(UserSignupDto userSignupModel);

	SigninResponseVO getUserDetailsAfterLogin(String username);

	String getOtp(String username);

	String updateUser(String username, UserUpdateRequestDto userUpdateRequestDto);

	String sendOtpForUpdatePassword(String username, UpdatePasswordOtpDto updatePasswordOtpDto);

	String updatePassword(String username, UpdatePasswordDto updatePasswordDto);

	String sendOtpForForgetPasswordByUsername(String username);

	String recoverAccountByUsername(
			ForgetPasswordChangePasswordByUsernameDto forgetPasswordChangePasswordByUsernameDto);

	String sendOtpForForgetPasswordByEmail(String email);

	String recoverAccountByEmail(ForgetPasswordChangePasswordByEmailDto forgetPasswordChangePasswordByEmailDto);

}