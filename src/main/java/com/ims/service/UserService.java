package com.ims.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.ims.dto.ForgetPasswordChangePasswordByEmailDto;
import com.ims.dto.ForgetPasswordChangePasswordByUsernameDto;
import com.ims.dto.SigninResponseVO;
import com.ims.dto.UpdatePasswordDto;
import com.ims.dto.UpdatePasswordOtpDto;
import com.ims.dto.UserSignupDto;
import com.ims.dto.UserUpdateRequestDto;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailFailedException;
import com.ims.exception.EmailNotFoundException;
import com.ims.exception.ExpiredOtpException;
import com.ims.exception.InvalidOtpException;
import com.ims.exception.InvalidUsernameException;
import com.ims.exception.PasswordMismatchException;
import com.ims.exception.UserNotFoundException;
import com.ims.exception.UsernameAlreadyExistsException;
import com.ims.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private EmailService emailService;
    
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Transactional
    public String signup(UserSignupDto userSignupModel) {
    	
    	log.info("In signup service "+userSignupModel);
    	
    	if (userRepository.existsByUsername(userSignupModel.getUsername())) {
    		log.error("Username is already taken"+userSignupModel.getUsername());
            throw new UsernameAlreadyExistsException("Username is already taken");
        }
    	
        UserEntity user = modelMapper.map(userSignupModel, UserEntity.class);
        StoreEntity store = modelMapper.map(userSignupModel, StoreEntity.class);

        user.setPassword(encoder.encode(userSignupModel.getPassword()));
        user.setRole("OWNER"); 

        store.setOwner(user);
        user.setStore(store);

        userRepository.save(user);
        
        try {
            String subject = "Welcome to IMS!";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                    "Your registration as an OWNER for " + store.getStoreName() + " is successful.\n\n" +
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
        	log.error("Failed to send welcome email: " + e.getMessage());
        }
        
        return "User registered successfully";
    }

	public SigninResponseVO getUserDetailsAfterLogin(String username) {
		log.info("In getUserDetailsAfterLogin service "+ username);
		if (!userRepository.existsByUsername(username)) {
			log.error("Username does not exists "+username);
            throw new UsernameNotFoundException("Username does not exists.");
        }
		UserEntity userEntity = userRepository.findByUsername(username);
		
		SigninResponseVO signInResponseVO = modelMapper.map(userEntity, SigninResponseVO.class);	    
	    modelMapper.map(userEntity.getStore(), signInResponseVO);
	    try {
            String subject = "Login detected to IMS!";
            String body = "Hello " + userEntity.getFullName() + ",\n\n" +
                    "Your Login is detected on "+new Date()+".\n\n" +
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(userEntity.getEmail(), subject, body);
        } catch (Exception e) {
        	log.error("Failed to send welcome email: " + e.getMessage());
        }
	    return signInResponseVO;
	}

	public String getOtp(String username) {
	    log.info("Generating secure OTP for: {}", username);
	    
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) throw new UserNotFoundException("User not found");

	    String generatedOtp = generateFourDigitOtp();

	    user.setOtp(generatedOtp);
	    user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
	    userRepository.save(user);

	    try {
	        String subject = "OTP for Profile Update";
	        String body = "Hello " + user.getFullName() + ",\n\n" +
	                      "Your verification code is: " + generatedOtp + "\n" +
	                      "This code will expire in 5 minutes.\n\n\n"+
            			  "Regards,\nInventory Management System";
	        emailService.sendSimpleEmail(user.getEmail(), subject, body);
	    } catch (Exception e) {
	        log.error("Email failed: {}", e.getMessage());
	        throw new EmailFailedException("Email service down. Try again later.");
	    }

	    return "OTP sent to your registered email.";
	}
	
	private String generateFourDigitOtp() {
	    Random random = new Random();
	    int otp = 1000 + random.nextInt(9000); 
	    return String.valueOf(otp);
	}

	@Transactional
	public String updateUser(String username, UserUpdateRequestDto userUpdateRequestDto) {
		log.info("In update user service "+ username +" "+userUpdateRequestDto);
		UserEntity user = userRepository.findByUsername(username);
		
		if (user.getOtp() == null || !user.getOtp().equals(userUpdateRequestDto.getOtp())) {
	        throw new InvalidOtpException("Invalid OTP provided.");
	    }
		
		if (user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
	        throw new ExpiredOtpException("OTP has expired. Please request a new one.");
	    }
		user.setFullName(userUpdateRequestDto.getFullName());
	    user.getStore().setStoreName(userUpdateRequestDto.getStoreName());
	    
	    user.setOtp(null);
	    user.setOtpExpiryTime(null);
	    
	    userRepository.save(user);
	    
	    try {
            String subject = "Profile Updated Successfully";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                          "your profile has been updated successfully\n\n\n" +
                          "Regards,\nInventory Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
	    
		return "User Updated successfully";
	}

	public String sendOtpForUpdatePassword(String username, UpdatePasswordOtpDto updatePasswordOtpDto) {
		log.info("In sendOtpForUpdatePassword service "+ username +" "+updatePasswordOtpDto);
		if(!username.equals(updatePasswordOtpDto.getUsername())) {
			throw new InvalidUsernameException("Invalid Username");
		}
		UserEntity user = userRepository.findByUsername(username);
	    if (user == null) throw new UserNotFoundException("User not found");
	    boolean isPasswordMatch = encoder.matches(updatePasswordOtpDto.getPassword(), user.getPassword());

        if (!isPasswordMatch) {
            log.warn("Password mismatch for user: {}", username);
            throw new PasswordMismatchException("Current password does not match. OTP not sent.");
        }
        String generatedOtp = generateFourDigitOtp();

        user.setOtp(generatedOtp);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        try {
            String subject = "Security Alert: Password Change OTP";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                          "You are attempting to change your password. Your OTP is: " + generatedOtp + "\n" +
                          "This code will expire in 5 minutes. If this wasn't you, change your password immediately.\n\n\n"+
            			  "Regards,\nInventory Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            throw new EmailFailedException("Email service failed. Please try again later.");
        }

        return "OTP sent to your registered email.";
	}
	
	@Transactional
	public String updatePassword(String username, UpdatePasswordDto updatePasswordDto) {
	    log.info("In update password service for user: {}", username);
	    
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) {
	        throw new UserNotFoundException("User not found");
	    }
	    
	    if(!username.equals(updatePasswordDto.getUsername())) {
			throw new InvalidUsernameException("Invalid Username");
		}
	    
	    boolean isPasswordMatch = encoder.matches(updatePasswordDto.getPassword(), user.getPassword());

	    if (!isPasswordMatch) {
            log.warn("Password mismatch for user: {}", username);
            throw new PasswordMismatchException("Current password does not match. OTP not sent.");
        }
	    
	    if (user.getOtp() == null || !user.getOtp().equals(updatePasswordDto.getOtp())) {
	        throw new InvalidOtpException("Invalid OTP provided.");
	    }

	    if (user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
	        throw new ExpiredOtpException("OTP has expired. Please request a new one.");
	    }

	    String encodedPassword = encoder.encode(updatePasswordDto.getNewPassword());
	    user.setPassword(encodedPassword);


	    user.setOtp(null);
	    user.setOtpExpiryTime(null);

	    userRepository.save(user);
	    
	    try {
            String subject = "Password is successfully updated.";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                          "your password has been updated successfully\n\n\n" +
                          "Regards,\nInventory Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
        }
	    
	    log.info("Password updated successfully for user: {}", username);
	    return "Password updated successfully! Please login with your new credentials.";
	}

	public String sendOtpForForgetPasswordByUsername(String username) {
	    log.info("In sendOtpForForgetPasswordByUsername service for user: {}", username);
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) {
	        throw new UserNotFoundException("User not found");
	    }
	    String email = user.getEmail();
	    if(email == null) {
	    	throw new EmailNotFoundException("Email not found");
	    }
	    String generatedOtp = generateFourDigitOtp();
	    user.setOtp(generatedOtp);
	    user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
	    userRepository.save(user);
	    try {
            String subject = "Security Alert: Password Change OTP";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                          "You are attempting to change your password. Your OTP is: " + generatedOtp + "\n" +
                          "This code will expire in 5 minutes. If this wasn't you, change your password immediately.\n\n\n"+
            			  "Regards,\nInventory Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            throw new EmailFailedException("Email service failed. Please try again later.");
        }
		return "Otp send successfully to registered email of user.";
	}

	public String recoverAccountByUsername(ForgetPasswordChangePasswordByUsernameDto forgetPasswordChangePasswordByUsernameDto) {
	    log.info("In recoverAccountByUsername service for user: {}", forgetPasswordChangePasswordByUsernameDto);
	    UserEntity user = userRepository.findByUsername(forgetPasswordChangePasswordByUsernameDto.getUsername());
	    if (user == null) {
	        throw new UserNotFoundException("User not found");
	    }
	    if(user.getOtp() == null || !user.getOtp().equals(forgetPasswordChangePasswordByUsernameDto.getOtp())) {
	    	throw new InvalidOtpException("Invalid Otp");
	    }
	    if(user.getOtpExpiryTime()!=null && user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
	    	throw new ExpiredOtpException("Otp is expired");
	    }
	    
	    String newPassword = encoder.encode(forgetPasswordChangePasswordByUsernameDto.getPassword());
	    user.setPassword(newPassword);
	    
	    user.setOtp(null);
	    user.setOtpExpiryTime(null);
	    
	    userRepository.save(user);
		return "Password updated successfully, please login again";
	}

	public String sendOtpForForgetPasswordByEmail(String email) {
	    log.info("In sendOtpForForgetPasswordByEmail service for user: {}", email);
	    UserEntity user = userRepository.findByEmail(email);
	    if (user == null) {
	        throw new UserNotFoundException("User not found");
	    }
	    String generatedOtp = generateFourDigitOtp();
	    user.setOtp(generatedOtp);
	    user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
	    userRepository.save(user);
	    try {
            String subject = "Security Alert: Password Change OTP";
            String body = "Hello " + user.getFullName() + ",\n\n" +
            			  "Your username is : "+user.getUsername()+" \n"+
                          "You are attempting to change your password. Your OTP is: " + generatedOtp + "\n" +
                          "This code will expire in 5 minutes. If this wasn't you, change your password immediately.\n\n\n"+
            			  "Regards,\nInventory Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            throw new EmailFailedException("Email service failed. Please try again later.");
        }
		return "Otp send successfully to registered email of user.";
	}

	public String recoverAccountByEmail(ForgetPasswordChangePasswordByEmailDto forgetPasswordChangePasswordByEmailDto) {
		log.info("In recoverAccountByEmail service for user: {}", forgetPasswordChangePasswordByEmailDto);
		UserEntity user = userRepository.findByEmail(forgetPasswordChangePasswordByEmailDto.getEmail());
	    if (user == null) {
	        throw new UserNotFoundException("User not found");
	    }
	    if(user.getOtp() == null || !user.getOtp().equals(forgetPasswordChangePasswordByEmailDto.getOtp())) {
	    	throw new InvalidOtpException("Invalid Otp");
	    }
	    if(user.getOtpExpiryTime()!=null && user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
	    	throw new ExpiredOtpException("Otp is expired");
	    }
	    
	    String newPassword = encoder.encode(forgetPasswordChangePasswordByEmailDto.getPassword());
	    user.setPassword(newPassword);
	    
	    user.setOtp(null);
	    user.setOtpExpiryTime(null);
	    
	    userRepository.save(user);
		return "Password updated successfully, please login again";
	}
}
