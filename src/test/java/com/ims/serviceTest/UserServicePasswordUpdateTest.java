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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ims.dto.UpdatePasswordDto;
import com.ims.dto.UpdatePasswordOtpDto;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailFailedException;
import com.ims.exception.ExpiredOtpException;
import com.ims.exception.InvalidOtpException;
import com.ims.exception.InvalidUsernameException;
import com.ims.exception.PasswordMismatchException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServicePasswordUpdateTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private UserEntity userEntity;
    private StoreEntity storeEntity;
    private String username;
    private UpdatePasswordOtpDto updatePasswordOtpDto;
    private UpdatePasswordDto updatePasswordDto;

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
        userEntity.setPassword("encodedOldPassword");
        userEntity.setRole("ROLE_OWNER");
        userEntity.setActive(true);
        userEntity.setStore(storeEntity);

        updatePasswordOtpDto = new UpdatePasswordOtpDto();
        updatePasswordOtpDto.setUsername(username);
        updatePasswordOtpDto.setPassword("oldPassword123");

        updatePasswordDto = new UpdatePasswordDto();
        updatePasswordDto.setUsername(username);
        updatePasswordDto.setPassword("oldPassword123");
        updatePasswordDto.setOtp("1234");
        updatePasswordDto.setNewPassword("newPassword456");
    }

    @Test
    void sendOtpForUpdatePassword_Success() {
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordOtpDto.getPassword(), userEntity.getPassword())).thenReturn(true);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        String result = userService.sendOtpForUpdatePassword(username, updatePasswordOtpDto);

        assertEquals("OTP sent to your registered email.", result);
        
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        
        UserEntity savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getOtp());
        assertEquals(4, savedUser.getOtp().length());
        assertNotNull(savedUser.getOtpExpiryTime());
        
        verify(userRepository).findByUsername(username);
        verify(encoder).matches(updatePasswordDto.getPassword(), "encodedOldPassword");
        verify(notificationService).sendPasswordOtp(any(UserEntity.class), anyString());
    }

    @Test
    void sendOtpForUpdatePassword_InvalidUsername_ThrowsException() {
        updatePasswordOtpDto.setUsername("differentUser");

        assertThrows(InvalidUsernameException.class, () -> {
            userService.sendOtpForUpdatePassword(username, updatePasswordOtpDto);
        });

        verify(userRepository, never()).findByUsername(any());
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordOtp(any(), any());
    }

    @Test
    void sendOtpForUpdatePassword_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername(username)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> {
            userService.sendOtpForUpdatePassword(username, updatePasswordOtpDto);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordOtp(any(), any());
    }

    @Test
    void sendOtpForUpdatePassword_PasswordMismatch_ThrowsException() {
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordOtpDto.getPassword(), userEntity.getPassword())).thenReturn(false);

        assertThrows(PasswordMismatchException.class, () -> {
            userService.sendOtpForUpdatePassword(username, updatePasswordOtpDto);
        });

        verify(userRepository).findByUsername(username);
        verify(encoder).matches(updatePasswordDto.getPassword(), "encodedOldPassword");
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordOtp(any(), any());
    }

    @Test
    void sendOtpForUpdatePassword_EmailSendFails_ThrowsException() {
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordOtpDto.getPassword(), userEntity.getPassword())).thenReturn(true);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        
        doThrow(new EmailFailedException("SMTP server down"))
            .when(notificationService).sendPasswordOtp(any(UserEntity.class), anyString());

        assertThrows(EmailFailedException.class, () -> {
            userService.sendOtpForUpdatePassword(username, updatePasswordOtpDto);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository).save(any(UserEntity.class));
        verify(notificationService).sendPasswordOtp(any(UserEntity.class), anyString());
    }


    @Test
    void updatePassword_Success() {
        userEntity.setOtp("1234");
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordDto.getPassword(), userEntity.getPassword())).thenReturn(true);
        when(encoder.encode(updatePasswordDto.getNewPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        // Act
        String result = userService.updatePassword(username, updatePasswordDto);

        // Assert
        assertEquals("Password updated successfully! Please login with your new credentials.", result);
        
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("encodedNewPassword", savedUser.getPassword());
        assertNull(savedUser.getOtp());
        assertNull(savedUser.getOtpExpiryTime());
        
        verify(userRepository).findByUsername(username);
        verify(encoder).matches(updatePasswordDto.getPassword(), "encodedOldPassword");
        verify(encoder).encode(updatePasswordDto.getNewPassword());
        verify(notificationService).sendPasswordUpdateConfirmation(userEntity);
    }

    @Test
    void updatePassword_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername(username)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> {
            userService.updatePassword(username, updatePasswordDto);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordUpdateConfirmation(any());
    }

    @Test
    void updatePassword_InvalidUsername_ThrowsException() {
        updatePasswordDto.setUsername("differentUser");
        when(userRepository.findByUsername(username)).thenReturn(userEntity);

        assertThrows(InvalidUsernameException.class, () -> {
            userService.updatePassword(username, updatePasswordDto);
        });

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordUpdateConfirmation(any());
    }

    @Test
    void updatePassword_PasswordMismatch_ThrowsException() {
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordDto.getPassword(), userEntity.getPassword())).thenReturn(false);

        assertThrows(PasswordMismatchException.class, () -> {
            userService.updatePassword(username, updatePasswordDto);
        });

        verify(userRepository).findByUsername(username);
        verify(encoder).matches(updatePasswordDto.getPassword(), "encodedOldPassword");
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordUpdateConfirmation(any());
    }

    @Test
    void updatePassword_InvalidOtp_ThrowsException() {
        userEntity.setOtp("5678");
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordDto.getPassword(), userEntity.getPassword())).thenReturn(true);

        assertThrows(InvalidOtpException.class, () -> {
            userService.updatePassword(username, updatePasswordDto);
        });

        verify(userRepository).findByUsername(username);
        verify(encoder).matches(updatePasswordDto.getPassword(), "encodedOldPassword");
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordUpdateConfirmation(any());
    }

    @Test
    void updatePassword_NullOtp_ThrowsException() {
        userEntity.setOtp(null);
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordDto.getPassword(), userEntity.getPassword())).thenReturn(true);

        assertThrows(InvalidOtpException.class, () -> {
            userService.updatePassword(username, updatePasswordDto);
        });

        verify(userRepository).findByUsername(username);
        verify(encoder).matches(updatePasswordDto.getPassword(), "encodedOldPassword");
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordUpdateConfirmation(any());
    }

    @Test
    void updatePassword_ExpiredOtp_ThrowsException() {
        userEntity.setOtp("1234");
        userEntity.setOtpExpiryTime(LocalDateTime.now().minusMinutes(1)); // Expired
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordDto.getPassword(), userEntity.getPassword())).thenReturn(true);

        assertThrows(ExpiredOtpException.class, () -> {
            userService.updatePassword(username, updatePasswordDto);
        });

        verify(userRepository).findByUsername(username);
        verify(encoder).matches(updatePasswordDto.getPassword(), "encodedOldPassword");
        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordUpdateConfirmation(any());
    }

    @Test
    void updatePassword_NotificationFails_StillUpdatesPassword() {
        userEntity.setOtp("1234");
        userEntity.setOtpExpiryTime(LocalDateTime.now().plusMinutes(3));
        
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(encoder.matches(updatePasswordDto.getPassword(), userEntity.getPassword())).thenReturn(true);
        when(encoder.encode(updatePasswordDto.getNewPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        
        doThrow(new EmailFailedException("Email service down"))
            .when(notificationService).sendPasswordUpdateConfirmation(userEntity);

        String result = userService.updatePassword(username, updatePasswordDto);

        assertEquals("Password updated successfully! Please login with your new credentials.", result);
        
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        
        UserEntity savedUser = userCaptor.getValue();
        assertEquals("encodedNewPassword", savedUser.getPassword());
        assertNull(savedUser.getOtp());
        assertNull(savedUser.getOtpExpiryTime());
        
        verify(notificationService).sendPasswordUpdateConfirmation(userEntity);
    }
}