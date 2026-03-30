package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ims.dto.UserUpdateRequestDto;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailFailedException;
import com.ims.exception.ExpiredOtpException;
import com.ims.exception.InvalidOtpException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceOtpAndUpdateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private UserEntity userEntity;
    private StoreEntity storeEntity;
    private String username;
    private UserUpdateRequestDto updateRequestDto;

    @BeforeEach
    void setUp() {
        username = "john";

        storeEntity = new StoreEntity();
        storeEntity.setStoreName("John's Store");
        storeEntity.setGstNumber("12ABCDE1234F1Z5");

        userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setFullName("John Doe");
        userEntity.setEmail("john@example.com");
        userEntity.setRole("ROLE_OWNER");
        userEntity.setActive(true);
        userEntity.setStore(storeEntity);

        updateRequestDto = new UserUpdateRequestDto();
        updateRequestDto.setOtp("1234");
        updateRequestDto.setFullName("John Updated");
        updateRequestDto.setStoreName("Updated Store");
    }

    // ==================== GET OTP TESTS ====================

    @Test
    void getOtp_Success() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        // Act
        String result = userService.getOtp(username);

        // Assert
        assertEquals("OTP sent to your registered email.", result);
        
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        
        UserEntity savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getOtp());
        assertEquals(4, savedUser.getOtp().length());
        assertNotNull(savedUser.getOtpExpiryTime());
        
        verify(userRepository).findByUsername(username);
        verify(notificationService).sendOtpNotification(any(UserEntity.class), anyString());
    }

    @Test
    void getOtp_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(null);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.getOtp(username);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendOtpNotification(any(), any());
    }

    @Test
    void getOtp_EmailSendFails_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        
        doThrow(new EmailFailedException("SMTP server down"))
            .when(notificationService).sendOtpNotification(any(UserEntity.class), anyString());

        // Act & Assert
        assertThrows(EmailFailedException.class, () -> {
            userService.getOtp(username);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository).save(any(UserEntity.class));
        verify(notificationService).sendOtpNotification(any(UserEntity.class), anyString());
    }

    // ==================== UPDATE USER TESTS ====================

    @Test
    void updateUser_Success() {
        // Arrange
        userEntity.setOtp("1234");
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        // Act
        String result = userService.updateUser(username, updateRequestDto);

        // Assert
        assertEquals("User Updated successfully", result);
        
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("John Updated", savedUser.getFullName());
        assertEquals("Updated Store", savedUser.getStore().getStoreName());
        assertNull(savedUser.getOtp());
        assertNull(savedUser.getOtpExpiryTime());
        
        verify(userRepository).findByUsername(username);
        verify(notificationService).sendProfileUpdateNotification(userEntity);
    }

    @Test
    void updateUser_InvalidOtp_ThrowsException() {
        // Arrange
        userEntity.setOtp("5678");
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);

        // Act & Assert
        assertThrows(InvalidOtpException.class, () -> {
            userService.updateUser(username, updateRequestDto);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendProfileUpdateNotification(any());
    }

    @Test
    void updateUser_NullOtp_ThrowsException() {
        // Arrange
        userEntity.setOtp(null);
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);

        // Act & Assert
        assertThrows(InvalidOtpException.class, () -> {
            userService.updateUser(username, updateRequestDto);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendProfileUpdateNotification(any());
    }

    @Test
    void updateUser_ExpiredOtp_ThrowsException() {
        // Arrange
        userEntity.setOtp("1234");
        userEntity.setOtpExpiryTime(LocalDateTime.now().minusMinutes(1)); // Expired 1 minute ago
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);

        // Act & Assert
        assertThrows(ExpiredOtpException.class, () -> {
            userService.updateUser(username, updateRequestDto);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendProfileUpdateNotification(any());
    }

    @Test
    void updateUser_NotificationFails_StillUpdatesUser() {
        // Arrange
        userEntity.setOtp("1234");
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        
        doThrow(new RuntimeException("Email service down"))
            .when(notificationService).sendProfileUpdateNotification(userEntity);

        // Act
        String result = userService.updateUser(username, updateRequestDto);

        // Assert
        assertEquals("User Updated successfully", result);
        
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("John Updated", savedUser.getFullName());
        assertEquals("Updated Store", savedUser.getStore().getStoreName());
        assertNull(savedUser.getOtp());
        assertNull(savedUser.getOtpExpiryTime());
        
        verify(userRepository).findByUsername(username);
        verify(notificationService).sendProfileUpdateNotification(userEntity);
    }

    @Test
    void updateUser_OnlyFullNameUpdated() {
        // Arrange
        updateRequestDto.setStoreName(null);
        userEntity.setOtp("1234");
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        // Act
        String result = userService.updateUser(username, updateRequestDto);

        // Assert
        assertEquals("User Updated successfully", result);
        
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("John Updated", savedUser.getFullName());
    }
}