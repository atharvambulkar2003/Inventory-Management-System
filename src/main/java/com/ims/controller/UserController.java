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

@CrossOrigin
@RestController
@RequestMapping("/api/user")
public class UserController {
	@Autowired
	private UserService userService;
	
	@Autowired 
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private JwtService jwtService;
	
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody UserSignupDto userSignupModel) {
		try {
			String response = userService.signup(userSignupModel);
			return new ResponseEntity<>(response,HttpStatus.CREATED);
		}catch(Exception e) {
			return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
		}
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginDto loginModel) throws AuthenticationException {
	    try {
	    	Authentication authentication = authenticationManager.authenticate(
    		    new UsernamePasswordAuthenticationToken(loginModel.getUsername(), loginModel.getPassword())
    		);

    		if (authentication.isAuthenticated()) {
    		    String token = jwtService.generateToken(loginModel.getUsername());
    		    SigninResponseVO response = userService.getUserDetailsAfterLogin(loginModel.getUsername());
    		    response.setToken(token);
    		    return ResponseEntity.ok(response); 
    		}else {
    			throw new BadCredentialsException("Invalid username or password");
    		}
	    }catch(BadCredentialsException exception) {
	    	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exception.getMessage());
	    }catch (Exception exception) {
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login");
	    }
	}
	
	@GetMapping("/all")
	public String getAll() {
		return "Hey Its working";
	}
	
}
