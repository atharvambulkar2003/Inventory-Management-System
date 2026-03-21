package com.ims.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ims.dto.LoginDto;
import com.ims.dto.SigninResponseVO;
import com.ims.dto.UserSignupDto;
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
		    String token = jwtService.generateToken(loginModel.getUsername());
		    SigninResponseVO response = userService.getUserDetailsAfterLogin(loginModel.getUsername());
		    response.setToken(token);
		    return ResponseEntity.ok(response); 
		}else {
			log.error("Login error, Invalid username or password");
			throw new BadCredentialsException("Invalid username or password");
		}
	}
	
	@GetMapping("/all")
	public String getAll() {
		return "Hey Its working";
	}
	
}
