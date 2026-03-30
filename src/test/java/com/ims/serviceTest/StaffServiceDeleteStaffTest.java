package com.ims.serviceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ims.entity.UserEntity;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.UserRepository;
import com.ims.service.StaffService;

@ExtendWith(MockitoExtension.class)
class StaffServiceDeleteStaffTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private StaffService staffService;

    private UserEntity owner;
    private UserEntity staff;

    @BeforeEach
    void setUp() {
        owner = new UserEntity();
        owner.setId(1L);
        owner.setUsername("owner1");

        staff = new UserEntity();
        staff.setId(2L);
        staff.setUsername("staffuser");
        staff.setActive(true);
    }

    @Test
    void deleteStaff_success_deactivatesStaff() {
        when(userRepository.findByUsername("owner1")).thenReturn(owner);
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));

        String result = staffService.deleteStaff(2L, "owner1");

        assertThat(result).isEqualTo("Staff account deactivated successfully");
        assertThat(staff.isActive()).isFalse();
        verify(userRepository).save(staff);
    }

    @Test
    void deleteStaff_ownerNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> staffService.deleteStaff(2L, "ghost"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteStaff_staffNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("owner1")).thenReturn(owner);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> staffService.deleteStaff(2L, "owner1"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteStaff_alreadyInactive_savesWithInactiveStatus() {
        staff.setActive(false);
        when(userRepository.findByUsername("owner1")).thenReturn(owner);
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));

        String result = staffService.deleteStaff(2L, "owner1");

        assertThat(result).isEqualTo("Staff account deactivated successfully");
        assertThat(staff.isActive()).isFalse();
        verify(userRepository).save(staff);
    }
}