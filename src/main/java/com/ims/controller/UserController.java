package com.ims.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ims.dto.ForgetPasswordChangePasswordByEmailDto;
import com.ims.dto.ForgetPasswordChangePasswordByUsernameDto;
import com.ims.dto.LoginDto;
import com.ims.dto.SigninResponseVO;
import com.ims.dto.UpdatePasswordDto;
import com.ims.dto.UpdatePasswordOtpDto;
import com.ims.dto.UserSignupDto;
import com.ims.dto.UserUpdateRequestDto;
import com.ims.service.UserService;
import com.ims.service.auth.JwtService;

import lombok.extern.slf4j.Slf4j;

@CrossOrigin
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {
	@Autowired
	private UserService userService;
	
	@Autowired 
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private JwtService jwtService;
	
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody UserSignupDto userSignupModel) {
		log.info("In signup "+userSignupModel);
		String response = userService.signup(userSignupModel);
		return new ResponseEntity<>(response,HttpStatus.CREATED);
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginDto loginModel) {
		log.info("In login "+loginModel);
    	Authentication authentication = authenticationManager.authenticate(
		    new UsernamePasswordAuthenticationToken(loginModel.getUsername(), loginModel.getPassword())
		);
		if (authentication.isAuthenticated()) {
		    SigninResponseVO response = userService.getUserDetailsAfterLogin(loginModel.getUsername());
		    if(!response.isActive()) {
		    	log.error("Login denied: Account is deactivated for {}", loginModel.getUsername());
	            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account has been deactivated.");
		    }
		    String token = jwtService.generateToken(loginModel.getUsername());
		    response.setToken(token);
			log.info("In login controller "+response);
		    return ResponseEntity.ok(response); 
		}else {
			log.error("Login error, Invalid username or password");
			throw new BadCredentialsException("Invalid username or password");
		}
	}
	
	@GetMapping
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<?> sendOtpToEmailForProfileUpdate(Authentication authentication){
		log.info("In send Otp "+authentication.getName());
		String username = authentication.getName();
		String message = userService.getOtp(username);
		return new ResponseEntity<>(message,HttpStatus.OK);
	}
	
	@PutMapping
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<?> updateUser(Authentication authentication,@RequestBody UserUpdateRequestDto userUpdateRequestDto){
		log.info("In update user "+authentication.getName());
		String username = authentication.getName();
		String message = userService.updateUser(username,userUpdateRequestDto);
		return new ResponseEntity<>(message,HttpStatus.OK);
	}
	
	@PostMapping("/otp")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<?> sendOtpForUpdatePassword(Authentication authentication,@RequestBody UpdatePasswordOtpDto updatePasswordOtpDto){
		log.info("In sendOtpForUpdatePassword "+authentication.getName());
		String username = authentication.getName();
		String message = userService.sendOtpForUpdatePassword(username,updatePasswordOtpDto);
		return new ResponseEntity<>(message,HttpStatus.OK);
	}
	
	@PutMapping("/password")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<?> updatePassword(Authentication authentication,@RequestBody UpdatePasswordDto updatePasswordDto){
		log.info("In update user "+authentication.getName());
		String username = authentication.getName();
		String message = userService.updatePassword(username,updatePasswordDto);
		return new ResponseEntity<>(message,HttpStatus.OK);
	}
	
	@GetMapping("/username/otp/{username}")
	public ResponseEntity<?> sendOtpForForgetPasswordByUsername(@PathVariable String username){
		log.info("In sendOtpForForgetPasswordByUsername "+username);
		String message = userService.sendOtpForForgetPasswordByUsername(username);
		return new ResponseEntity<>(message,HttpStatus.OK); 
	}
	
	@PutMapping("/username")
	public ResponseEntity<?> RecoverAccountByUsername(@RequestBody ForgetPasswordChangePasswordByUsernameDto forgetPasswordChangePasswordByUsernameDto){
		log.info("In RecoverAccountByUsername "+forgetPasswordChangePasswordByUsernameDto);
		String message = userService.recoverAccountByUsername(forgetPasswordChangePasswordByUsernameDto);
		return new ResponseEntity<>(message,HttpStatus.OK); 
	}
	
	@GetMapping("/email/otp/{email}")
	public ResponseEntity<?> sendOtpForForgetPasswordByEmail(@PathVariable String email){
		log.info("In sendOtpForForgetPasswordByEmail "+email);
		String message = userService.sendOtpForForgetPasswordByEmail(email);
		return new ResponseEntity<>(message,HttpStatus.OK); 
	}
	
	@PutMapping("/email")
	public ResponseEntity<?> RecoverAccountByEmail(@RequestBody ForgetPasswordChangePasswordByEmailDto forgetPasswordChangePasswordByEmailDto){
		log.info("In RecoverAccountByEmail "+forgetPasswordChangePasswordByEmailDto);
		String message = userService.recoverAccountByEmail(forgetPasswordChangePasswordByEmailDto);
		return new ResponseEntity<>(message,HttpStatus.OK); 
	}
	
}
