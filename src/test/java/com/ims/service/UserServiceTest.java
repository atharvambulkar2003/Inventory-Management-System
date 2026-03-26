package com.ims.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ims.dto.UserSignupDto;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.UsernameAlreadyExistsException;
import com.ims.repository.StoreRepository;
import com.ims.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private UserSignupDto signupDto;

    @BeforeEach
    void setUp() {
        signupDto = new UserSignupDto();
        signupDto.setUsername("atharva_test");
        signupDto.setEmail("test@gmail.com");
        signupDto.setPassword("password123");
        signupDto.setGstNumber("27ABCDE1234F1Z5");
    }

    @Test
    void testSignup_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(storeRepository.existsByGstNumber(anyString())).thenReturn(false);
        
        UserEntity user = new UserEntity();
        StoreEntity store = new StoreEntity();
        
        when(modelMapper.map(any(UserSignupDto.class), eq(UserEntity.class))).thenReturn(user);
        when(modelMapper.map(any(UserSignupDto.class), eq(StoreEntity.class))).thenReturn(store);
        when(storeRepository.save(any(StoreEntity.class))).thenReturn(store);
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        // Act
        String result = userService.signup(signupDto);

        // Assert
        assertEquals("User registered successfully", result);
        verify(userRepository, times(1)).save(any(UserEntity.class));
        verify(storeRepository, times(2)).save(any(StoreEntity.class));
    }

    @Test
    void testSignup_ThrowsUsernameExistsException() {
        // Arrange
        when(userRepository.existsByUsername("atharva_test")).thenReturn(true);

        // Act & Assert
        assertThrows(UsernameAlreadyExistsException.class, () -> {
            userService.signup(signupDto);
        });
        
        // Verify save was NEVER called
        verify(userRepository, never()).save(any(UserEntity.class));
    }
}