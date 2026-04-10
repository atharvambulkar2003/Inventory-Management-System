package com.ims.service;

import java.time.LocalDateTime;
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
import com.ims.exception.EmailAlreadyExistsException;
import com.ims.exception.EmailFailedException;
import com.ims.exception.EmailNotFoundException;
import com.ims.exception.ExpiredOtpException;
import com.ims.exception.GstNumberAlreadyExistsException;
import com.ims.exception.InvalidOtpException;
import com.ims.exception.InvalidUsernameException;
import com.ims.exception.PasswordMismatchException;
import com.ims.exception.UserNotFoundException;
import com.ims.exception.UsernameAlreadyExistsException;
import com.ims.repository.BatchRepository;
import com.ims.repository.StoreRepository;
import com.ims.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private NotificationService notificationService;
   
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Override
	@Transactional
    public String signup(UserSignupDto userSignupModel) {
    	
    	log.info("In signup service "+userSignupModel);
    	
    	if (userRepository.existsByUsername(userSignupModel.getUsername())) {
    		log.error("Username is already taken"+userSignupModel.getUsername());
            throw new UsernameAlreadyExistsException("Username is already taken");
        }
    	
    	if (userRepository.existsByEmail(userSignupModel.getEmail())) {
    		log.error("Email is already exists"+userSignupModel.getEmail());
            throw new EmailAlreadyExistsException("Email is already exists "+userSignupModel.getEmail());
        }
    	
    	if (storeRepository.existsByGstNumber(userSignupModel.getGstNumber())) {
    	    log.error("GST Number already exists: " + userSignupModel.getGstNumber());
    	    throw new GstNumberAlreadyExistsException("GST Number " + userSignupModel.getGstNumber() + " is already registered");
    	}
    	
        UserEntity user = modelMapper.map(userSignupModel, UserEntity.class);
        StoreEntity store = modelMapper.map(userSignupModel, StoreEntity.class);

        user.setPassword(encoder.encode(userSignupModel.getPassword()));
        user.setRole("ROLE_OWNER"); 
        user.setActive(true);
        
        StoreEntity savedStore = storeRepository.save(store);
        
        user.setStore(savedStore);
        UserEntity savedUser = userRepository.save(user);
        savedStore.setOwner(savedUser);
        storeRepository.save(savedStore);        
        try {
            notificationService.sendOwnerWelcomeNotification(savedUser, savedStore);
        } catch (Exception e) {
            log.error("Notification failed: {}", e.getMessage());
        }
        
        return "User registered successfully";
    }

	@Override
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
	        notificationService.sendLoginAlert(userEntity);
	        log.info("Send login alert to {}", userEntity);
	    } catch (Exception e) {
	        log.error("Could not send login alert: {}", e.getMessage());
	    }
	    return signInResponseVO;
	}

	@Override
	@Transactional
	public String getOtp(String username) {
	    log.info("Generating secure OTP for: {}", username);
	    
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) throw new UserNotFoundException("User not found");

	    String generatedOtp = generateFourDigitOtp();

	    user.setOtp(generatedOtp);
	    user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
	    userRepository.save(user);

	    try {	        
	        notificationService.sendOtpNotification(user, generatedOtp);
	        log.info("OTP sent successfully to {}", user.getEmail());
	    } catch (EmailFailedException e) {
	        log.error("Failed to send OTP email: {}", e.getMessage());
	        throw new EmailFailedException("Unable to send opt, please check the connection");
	    }

	    return "OTP sent to your registered email.";
	}
	
	private String generateFourDigitOtp() {
	    Random random = new Random();
	    int otp = 1000 + random.nextInt(9000); 
	    return String.valueOf(otp);
	}

	@Override
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
	        notificationService.sendProfileUpdateNotification(user);
	        log.info("profile update confirmation sent to {}", user.getEmail());
	    } catch (Exception e) {
	        log.error("Notification failed: {}", e.getMessage());
	    }
	    
		return "User Updated successfully";
	}

	@Override
	@Transactional
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
            notificationService.sendPasswordOtp(user, generatedOtp);
            log.info("Password OTP successfully sent to: {}", user.getEmail());
        } catch (EmailFailedException e) {
            log.error("Failed to send Password OTP email: {}", e.getMessage());
            throw new EmailFailedException("Email service is temporarily unavailable. Please try again later.");
        }

        return "OTP sent to your registered email.";
	}
	
	@Override
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
	        notificationService.sendPasswordUpdateConfirmation(user);
	        log.info("Password update confirmation sent to {}", user.getEmail());
	    } catch (EmailFailedException e) {
	        log.error("Failed to send password update confirmation: {}", e.getMessage());
	    }
	    
	    log.info("Password updated successfully for user: {}", username);
	    return "Password updated successfully! Please login with your new credentials.";
	}

	@Override
	@Transactional
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
	        notificationService.forgotPasswordOtp(user, generatedOtp);
	        log.info("Password OTP successfully sent to: {}", user.getEmail());
	    } catch (EmailFailedException e) {
	        log.error("Failed to send Password OTP email: {}", e.getMessage());
	        throw new EmailFailedException("Email service is temporarily unavailable. Please try again later.");
	    }
		return "Otp send successfully to registered email of user.";
	}

	@Override
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

	@Override
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
	        notificationService.sendPasswordOtpForEmail(user, generatedOtp);
	        log.info("Security OTP sent to username: {}", user.getUsername());
	    } catch (EmailFailedException e) {
	        log.error("Failed to send Security OTP: {}", e.getMessage());
	        throw new EmailFailedException("Email service unavailable. Please try again in a few minutes.");
	    }
		return "Otp send successfully to registered email of user.";
	}

	@Override
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
