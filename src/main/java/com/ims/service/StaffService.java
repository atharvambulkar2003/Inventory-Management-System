package com.ims.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.ims.dto.AddStaffDto;
import com.ims.dto.StaffVO;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailAlreadyExistsException;
import com.ims.exception.EmailFailedException;
import com.ims.exception.UnauthorizedException;
import com.ims.exception.UserNotFoundException;
import com.ims.exception.UsernameAlreadyExistsException;
import com.ims.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StaffService {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private NotificationService notificationService;

	@Transactional
	public String addStaffToStore(String username, AddStaffDto addStaffDto) {
		log.info("In addStaffToStore service "+ username + " adding staff "+ addStaffDto);
		if (userRepository.existsByUsername(addStaffDto.getUsername())) {
    		log.error("Username is already taken"+addStaffDto.getUsername());
            throw new UsernameAlreadyExistsException("Username is already taken");
        }
		if (userRepository.existsByEmail(addStaffDto.getEmail())) {
    		log.error("Email is already exists"+addStaffDto.getEmail());
            throw new EmailAlreadyExistsException("Email is already exists "+addStaffDto.getEmail());
        }
		UserEntity userEntity = userRepository.findByUsername(username);
		if(userEntity == null) {
			throw new UserNotFoundException("User not found");
		}
		UserEntity staff = modelMapper.map(addStaffDto, UserEntity.class);
		staff.setPassword(passwordEncoder.encode(staff.getPassword()));
		staff.setRole("ROLE_STAFF");
		staff.setActive(true);
		staff.setStore(userEntity.getStore());
		userEntity.getStore().getStaff().add(staff);
		userRepository.save(staff);
		try {
		    notificationService.sendStaffOnboardingNotifications(userEntity, staff, addStaffDto.getPassword());
		    log.info("Onboarding emails sent for staff: {}", staff.getUsername());
		} catch (EmailFailedException e) {
		    log.error("Notification failed: {}", e.getMessage());
		}
		return "Staff added successfully";
	}

	public List<StaffVO> getAllStaff(String username) {
		log.info("In getAllStaff service "+ username);
		UserEntity userEntity = userRepository.findByUsername(username);
		if(userEntity == null) {
			throw new UserNotFoundException("User not found");
		}
		List<UserEntity> staff = userEntity.getStore().getStaff();

	    return staff.stream()
	    			.filter(user -> "ROLE_STAFF".equals(user.getRole()) && user.isActive())
	                .map(entity -> modelMapper.map(entity, StaffVO.class))
	                .collect(Collectors.toList());
	}

	public String deleteStaff(Long id, String username) {
	    log.info("In deleteStaff service. Attempting to delete staff ID: " + id);
	    
	    UserEntity owner = userRepository.findByUsername(username);
	    if (owner == null) {
	        throw new UserNotFoundException("Owner not found");
	    }

	    UserEntity staff = userRepository.findById(id)
	            .orElseThrow(() -> new UserNotFoundException("Staff not found"));

	    if (!staff.getStore().getId().equals(owner.getStore().getId())) {
	        throw new UnauthorizedException("You can only delete staff from your store.");
	    }

	    userRepository.delete(staff); 
	    
	    return "Staff account permanently deleted successfully";
	}
	
}
