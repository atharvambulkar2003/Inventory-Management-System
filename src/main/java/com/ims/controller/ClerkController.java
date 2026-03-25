package com.ims.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ims.dto.AddStaffDto;
import com.ims.dto.StaffVO;
import com.ims.service.StaffService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/staff")
@Slf4j
public class ClerkController {
	
	@Autowired
	private StaffService staffService;
	
	@PostMapping("/addstaff")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<?> addStaffToStore(@RequestBody AddStaffDto addStaffDto,Authentication authentication){
		log.info("In addStaffToStore controller "+authentication.getName()+" adding staff "+addStaffDto);
		String username = authentication.getName();
		String message = staffService.addStaffToStore(username,addStaffDto);
		return new ResponseEntity<>(message,HttpStatus.CREATED);
	}
	
	@GetMapping("/getstaff")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<?> getAllStaff(Authentication authentication){
		log.info("In getAllStaff controller "+authentication.getName());
		String username = authentication.getName();
		List<StaffVO> allStaff = staffService.getAllStaff(username);
		return new ResponseEntity<>(allStaff,HttpStatus.OK);
	}
	
	@DeleteMapping("/deletestaff/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<?> deleteStaff(@PathVariable Long id,Authentication authentication){
		log.info("In deleteStaff controller "+authentication.getName()+" id = "+id);
		String username = authentication.getName();
		String message = staffService.deleteStaff(id,username);
		return new ResponseEntity<>(message,HttpStatus.OK);
	}
	
}
