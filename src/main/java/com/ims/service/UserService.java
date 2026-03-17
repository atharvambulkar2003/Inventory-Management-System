package com.ims.service;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.ims.dto.SigninResponseVO;
import com.ims.dto.UserSignupDto;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
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
    	
    	if (userRepository.existsByUsername(userSignupModel.getUsername())) {
            throw new RuntimeException("Username is already taken");
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
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
        
        return "User registered successfully";
    }

	public SigninResponseVO getUserDetailsAfterLogin(String username) {
		if (!userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username does not exists.");
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
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
	    return signInResponseVO;
	}
}
