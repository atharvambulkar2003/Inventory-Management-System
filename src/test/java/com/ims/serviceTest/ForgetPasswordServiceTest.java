package com.ims.serviceTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ims.dto.ForgetPasswordChangePasswordByEmailDto;
import com.ims.dto.ForgetPasswordChangePasswordByUsernameDto;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailFailedException;
import com.ims.exception.EmailNotFoundException;
import com.ims.exception.ExpiredOtpException;
import com.ims.exception.InvalidOtpException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.UserService;

@ExtendWith(MockitoExtension.class)
class ForgetPasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BCryptPasswordEncoder encoder;

    @InjectMocks
    private UserService userService; 

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setOtp("1234");
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
    }

    @Test
    void sendOtpForForgetPasswordByUsername_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);

        String result = userService.sendOtpForForgetPasswordByUsername("testuser");

        assertEquals("Otp send successfully to registered email of user.", result);
        verify(userRepository).save(user);
        verify(notificationService).forgotPasswordOtp(eq(user), anyString());
    }

    @Test
    void sendOtpForForgetPasswordByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> userService.sendOtpForForgetPasswordByUsername("unknown"));

        verify(userRepository, never()).save(any());
        verify(notificationService, never()).forgotPasswordOtp(any(), any());
    }

    @Test
    void sendOtpForForgetPasswordByUsername_EmailNotFound_ThrowsException() {
        user.setEmail(null);
        when(userRepository.findByUsername("testuser")).thenReturn(user);

        assertThrows(EmailNotFoundException.class,
                () -> userService.sendOtpForForgetPasswordByUsername("testuser"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void sendOtpForForgetPasswordByUsername_EmailServiceFails_ThrowsEmailFailedException() {
        when(userRepository.findByUsername("testuser")).thenReturn(user);
        doThrow(new EmailFailedException("SMTP error"))
                .when(notificationService).forgotPasswordOtp(any(), anyString());

        assertThrows(EmailFailedException.class,
                () -> userService.sendOtpForForgetPasswordByUsername("testuser"));
    }

    @Test
    void recoverAccountByUsername_Success() {
        ForgetPasswordChangePasswordByUsernameDto dto = new ForgetPasswordChangePasswordByUsernameDto();
        dto.setUsername("testuser");
        dto.setOtp("1234");
        dto.setPassword("newPassword123");

        when(userRepository.findByUsername("testuser")).thenReturn(user);
        when(encoder.encode("newPassword123")).thenReturn("encodedPassword");

        String result = userService.recoverAccountByUsername(dto);

        assertEquals("Password updated successfully, please login again", result);
        assertNull(user.getOtp());
        assertNull(user.getOtpExpiryTime());
        assertEquals("encodedPassword", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void recoverAccountByUsername_UserNotFound_ThrowsException() {
        ForgetPasswordChangePasswordByUsernameDto dto = new ForgetPasswordChangePasswordByUsernameDto();
        dto.setUsername("unknown");

        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> userService.recoverAccountByUsername(dto));
    }

    @Test
    void recoverAccountByUsername_InvalidOtp_ThrowsException() {
        ForgetPasswordChangePasswordByUsernameDto dto = new ForgetPasswordChangePasswordByUsernameDto();
        dto.setUsername("testuser");
        dto.setOtp("9999"); // wrong OTP

        when(userRepository.findByUsername("testuser")).thenReturn(user);

        assertThrows(InvalidOtpException.class,
                () -> userService.recoverAccountByUsername(dto));
    }

    @Test
    void recoverAccountByUsername_NullOtpInDb_ThrowsException() {
        user.setOtp(null);
        ForgetPasswordChangePasswordByUsernameDto dto = new ForgetPasswordChangePasswordByUsernameDto();
        dto.setUsername("testuser");
        dto.setOtp("1234");

        when(userRepository.findByUsername("testuser")).thenReturn(user);

        assertThrows(InvalidOtpException.class,
                () -> userService.recoverAccountByUsername(dto));
    }

    @Test
    void recoverAccountByUsername_ExpiredOtp_ThrowsException() {
        user.setOtpExpiryTime(LocalDateTime.now().minusMinutes(1)); // already expired
        ForgetPasswordChangePasswordByUsernameDto dto = new ForgetPasswordChangePasswordByUsernameDto();
        dto.setUsername("testuser");
        dto.setOtp("1234");

        when(userRepository.findByUsername("testuser")).thenReturn(user);

        assertThrows(ExpiredOtpException.class,
                () -> userService.recoverAccountByUsername(dto));
    }

    @Test
    void sendOtpForForgetPasswordByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);

        String result = userService.sendOtpForForgetPasswordByEmail("test@example.com");

        assertEquals("Otp send successfully to registered email of user.", result);
        verify(userRepository).save(user);
        verify(notificationService).sendPasswordOtpForEmail(eq(user), anyString());
    }

    @Test
    void sendOtpForForgetPasswordByEmail_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> userService.sendOtpForForgetPasswordByEmail("unknown@example.com"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void sendOtpForForgetPasswordByEmail_EmailServiceFails_ThrowsEmailFailedException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(user);
        doThrow(new EmailFailedException("SMTP error"))
                .when(notificationService).sendPasswordOtpForEmail(any(), anyString());

        assertThrows(EmailFailedException.class,
                () -> userService.sendOtpForForgetPasswordByEmail("test@example.com"));
    }

    @Test
    void recoverAccountByEmail_Success() {
        ForgetPasswordChangePasswordByEmailDto dto = new ForgetPasswordChangePasswordByEmailDto();
        dto.setEmail("test@example.com");
        dto.setOtp("1234");
        dto.setPassword("newPassword123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(user);
        when(encoder.encode("newPassword123")).thenReturn("encodedPassword");

        String result = userService.recoverAccountByEmail(dto);

        assertEquals("Password updated successfully, please login again", result);
        assertNull(user.getOtp());
        assertNull(user.getOtpExpiryTime());
        assertEquals("encodedPassword", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void recoverAccountByEmail_UserNotFound_ThrowsException() {
        ForgetPasswordChangePasswordByEmailDto dto = new ForgetPasswordChangePasswordByEmailDto();
        dto.setEmail("unknown@example.com");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> userService.recoverAccountByEmail(dto));
    }

    @Test
    void recoverAccountByEmail_InvalidOtp_ThrowsException() {
        ForgetPasswordChangePasswordByEmailDto dto = new ForgetPasswordChangePasswordByEmailDto();
        dto.setEmail("test@example.com");
        dto.setOtp("0000"); // wrong OTP

        when(userRepository.findByEmail("test@example.com")).thenReturn(user);

        assertThrows(InvalidOtpException.class,
                () -> userService.recoverAccountByEmail(dto));
    }

    @Test
    void recoverAccountByEmail_NullOtpInDb_ThrowsException() {
        user.setOtp(null);
        ForgetPasswordChangePasswordByEmailDto dto = new ForgetPasswordChangePasswordByEmailDto();
        dto.setEmail("test@example.com");
        dto.setOtp("1234");

        when(userRepository.findByEmail("test@example.com")).thenReturn(user);

        assertThrows(InvalidOtpException.class,
                () -> userService.recoverAccountByEmail(dto));
    }

    @Test
    void recoverAccountByEmail_ExpiredOtp_ThrowsException() {
        user.setOtpExpiryTime(LocalDateTime.now().minusMinutes(1)); // already expired
        ForgetPasswordChangePasswordByEmailDto dto = new ForgetPasswordChangePasswordByEmailDto();
        dto.setEmail("test@example.com");
        dto.setOtp("1234");

        when(userRepository.findByEmail("test@example.com")).thenReturn(user);

        assertThrows(ExpiredOtpException.class,
                () -> userService.recoverAccountByEmail(dto));
    }
}