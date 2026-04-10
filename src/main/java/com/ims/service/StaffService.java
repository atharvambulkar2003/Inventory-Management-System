package com.ims.service;

import java.util.List;

import com.ims.dto.AddStaffDto;
import com.ims.dto.StaffVO;

import jakarta.transaction.Transactional;

public interface StaffService {

	String addStaffToStore(String username, AddStaffDto addStaffDto);

	List<StaffVO> getAllStaff(String username);

	String deleteStaff(Long id, String username);

}