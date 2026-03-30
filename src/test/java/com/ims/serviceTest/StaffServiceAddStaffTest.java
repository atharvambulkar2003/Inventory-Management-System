package com.ims.serviceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ims.dto.AddStaffDto;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailAlreadyExistsException;
import com.ims.exception.EmailFailedException;
import com.ims.exception.UserNotFoundException;
import com.ims.exception.UsernameAlreadyExistsException;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.StaffService;

@ExtendWith(MockitoExtension.class)
class StaffServiceAddStaffTest {

    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ModelMapper modelMapper;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @InjectMocks private StaffService staffService;

    private UserEntity owner;
    private StoreEntity store;
    private AddStaffDto dto;
    private UserEntity mappedStaff;

    @BeforeEach
    void setUp() {
        store = new StoreEntity();
        store.setId(1L);
        store.setStaff(new ArrayList<>());

        owner = new UserEntity();
        owner.setUsername("owner1");
        owner.setStore(store);

        dto = new AddStaffDto();
        dto.setUsername("staffuser");
        dto.setFullName("Staff User");
        dto.setPassword("plainpass");
        dto.setEmail("staff@example.com");

        mappedStaff = new UserEntity();
        mappedStaff.setUsername("staffuser");
        mappedStaff.setEmail("staff@example.com");
        mappedStaff.setPassword("plainpass");
    }

    @Test
    void addStaffToStore_success_savesStaffAndSendsNotification() throws Exception {
        when(userRepository.existsByUsername("staffuser")).thenReturn(false);
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
        when(userRepository.findByUsername("owner1")).thenReturn(owner);
        when(modelMapper.map(dto, UserEntity.class)).thenReturn(mappedStaff);
        when(passwordEncoder.encode("plainpass")).thenReturn("encodedpass");

        String result = staffService.addStaffToStore("owner1", dto);

        assertThat(result).isEqualTo("Staff added successfully");
        assertThat(mappedStaff.getPassword()).isEqualTo("encodedpass");
        assertThat(mappedStaff.getRole()).isEqualTo("ROLE_STAFF");
        assertThat(mappedStaff.isActive()).isTrue();
        assertThat(mappedStaff.getStore()).isEqualTo(store);
        assertThat(store.getStaff()).contains(mappedStaff);
        verify(userRepository).save(mappedStaff);
        verify(notificationService).sendStaffOnboardingNotifications(owner, mappedStaff, "plainpass");
    }

    @Test
    void addStaffToStore_usernameAlreadyExists_throwsUsernameAlreadyExistsException() {
        when(userRepository.existsByUsername("staffuser")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> staffService.addStaffToStore("owner1", dto));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(notificationService, modelMapper, passwordEncoder);
    }

    @Test
    void addStaffToStore_emailAlreadyExists_throwsEmailAlreadyExistsException() {
        when(userRepository.existsByUsername("staffuser")).thenReturn(false);
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> staffService.addStaffToStore("owner1", dto));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(notificationService, modelMapper, passwordEncoder);
    }

    @Test
    void addStaffToStore_ownerNotFound_throwsUserNotFoundException() {
        when(userRepository.existsByUsername("staffuser")).thenReturn(false);
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
        when(userRepository.findByUsername("owner1")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> staffService.addStaffToStore("owner1", dto));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void addStaffToStore_emailFails_stillSavesStaff() throws Exception {
        when(userRepository.existsByUsername("staffuser")).thenReturn(false);
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
        when(userRepository.findByUsername("owner1")).thenReturn(owner);
        when(modelMapper.map(dto, UserEntity.class)).thenReturn(mappedStaff);
        when(passwordEncoder.encode("plainpass")).thenReturn("encodedpass");
        doThrow(new EmailFailedException("SMTP down"))
                .when(notificationService)
                .sendStaffOnboardingNotifications(any(), any(), any());

        String result = staffService.addStaffToStore("owner1", dto);

        assertThat(result).isEqualTo("Staff added successfully");
        verify(userRepository).save(mappedStaff);
    }

    @Test
    void addStaffToStore_passwordIsEncoded() throws Exception {
        when(userRepository.existsByUsername("staffuser")).thenReturn(false);
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
        when(userRepository.findByUsername("owner1")).thenReturn(owner);
        when(modelMapper.map(dto, UserEntity.class)).thenReturn(mappedStaff);
        when(passwordEncoder.encode("plainpass")).thenReturn("encodedpass");

        staffService.addStaffToStore("owner1", dto);

        assertThat(mappedStaff.getPassword()).isEqualTo("encodedpass");
        assertThat(mappedStaff.getPassword()).isNotEqualTo("plainpass");
        verify(passwordEncoder).encode("plainpass");
    }
}