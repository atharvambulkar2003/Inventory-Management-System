package com.ims.serviceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.ims.dto.StaffVO;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.UserRepository;
import com.ims.service.StaffService;

@ExtendWith(MockitoExtension.class)
class StaffServiceGetAllStaffTest {

    @Mock private UserRepository userRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private StaffService staffService;

    private UserEntity owner;
    private StoreEntity store;

    @BeforeEach
    void setUp() {
        store = new StoreEntity();
        store.setId(1L);

        owner = new UserEntity();
        owner.setUsername("owner1");
        owner.setStore(store);
    }

    private UserEntity buildStaffUser(Long id, boolean active, String role) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setRole(role);
        u.setActive(active);
        return u;
    }

    @Test
    void getAllStaff_success_returnsOnlyActiveStaff() {
        UserEntity activeStaff   = buildStaffUser(1L, true,  "ROLE_STAFF");
        UserEntity inactiveStaff = buildStaffUser(2L, false, "ROLE_STAFF");
        UserEntity ownerUser     = buildStaffUser(3L, true,  "ROLE_OWNER");
        store.setStaff(List.of(activeStaff, inactiveStaff, ownerUser));

        StaffVO vo = new StaffVO();
        vo.setId(1L);
        when(userRepository.findByUsername("owner1")).thenReturn(owner);
        when(modelMapper.map(activeStaff, StaffVO.class)).thenReturn(vo);

        List<StaffVO> result = staffService.getAllStaff("owner1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(modelMapper, times(1)).map(any(), eq(StaffVO.class));
    }

    @Test
    void getAllStaff_noStaff_returnsEmptyList() {
        store.setStaff(new ArrayList<>());
        when(userRepository.findByUsername("owner1")).thenReturn(owner);

        List<StaffVO> result = staffService.getAllStaff("owner1");

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }

    @Test
    void getAllStaff_onlyInactiveStaff_returnsEmptyList() {
        UserEntity inactiveStaff = buildStaffUser(1L, false, "ROLE_STAFF");
        store.setStaff(List.of(inactiveStaff));
        when(userRepository.findByUsername("owner1")).thenReturn(owner);

        List<StaffVO> result = staffService.getAllStaff("owner1");

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }

    @Test
    void getAllStaff_onlyOwnerRole_returnsEmptyList() {
        UserEntity ownerUser = buildStaffUser(1L, true, "ROLE_OWNER");
        store.setStaff(List.of(ownerUser));
        when(userRepository.findByUsername("owner1")).thenReturn(owner);

        List<StaffVO> result = staffService.getAllStaff("owner1");

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }

    @Test
    void getAllStaff_multipleActiveStaff_returnsAll() {
        UserEntity staff1 = buildStaffUser(1L, true, "ROLE_STAFF");
        UserEntity staff2 = buildStaffUser(2L, true, "ROLE_STAFF");
        store.setStaff(List.of(staff1, staff2));

        StaffVO vo1 = new StaffVO(); vo1.setId(1L);
        StaffVO vo2 = new StaffVO(); vo2.setId(2L);
        when(userRepository.findByUsername("owner1")).thenReturn(owner);
        when(modelMapper.map(staff1, StaffVO.class)).thenReturn(vo1);
        when(modelMapper.map(staff2, StaffVO.class)).thenReturn(vo2);

        List<StaffVO> result = staffService.getAllStaff("owner1");

        assertThat(result).hasSize(2);
        verify(modelMapper, times(2)).map(any(), eq(StaffVO.class));
    }

    @Test
    void getAllStaff_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> staffService.getAllStaff("ghost"));

        verifyNoInteractions(modelMapper);
    }
}