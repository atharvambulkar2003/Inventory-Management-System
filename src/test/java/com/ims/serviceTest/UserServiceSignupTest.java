package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.ims.exception.EmailAlreadyExistsException;
import com.ims.exception.GstNumberAlreadyExistsException;
import com.ims.exception.UsernameAlreadyExistsException;
import com.ims.repository.StoreRepository;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceSignupTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private UserSignupDto userSignupDto;
    private UserEntity userEntity;
    private StoreEntity storeEntity;

    @BeforeEach
    void setUp() {
        userSignupDto = new UserSignupDto();
        userSignupDto.setUsername("john");
        userSignupDto.setFullName("John Doe");
        userSignupDto.setPassword("password123");
        userSignupDto.setEmail("john@example.com");
        userSignupDto.setStoreName("John's Store");
        userSignupDto.setGstNumber("12ABCDE1234F1Z5");

        userEntity = new UserEntity();
        userEntity.setUsername("john");
        userEntity.setFullName("John Doe");
        userEntity.setEmail("john@example.com");

        storeEntity = new StoreEntity();
        storeEntity.setStoreName("John's Store");
        storeEntity.setGstNumber("12ABCDE1234F1Z5");
    }

    @Test
    void signup_Success() {
        when(userRepository.existsByUsername(userSignupDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(userSignupDto.getEmail())).thenReturn(false);
        when(storeRepository.existsByGstNumber(userSignupDto.getGstNumber())).thenReturn(false);
        
        when(modelMapper.map(userSignupDto, UserEntity.class)).thenReturn(userEntity);
        when(modelMapper.map(userSignupDto, StoreEntity.class)).thenReturn(storeEntity);
        
        when(encoder.encode(userSignupDto.getPassword())).thenReturn("encodedPassword");
        when(storeRepository.save(any(StoreEntity.class))).thenReturn(storeEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        String result = userService.signup(userSignupDto);

        assertEquals("User registered successfully", result);
        
        verify(userRepository).existsByUsername(userSignupDto.getUsername());
        verify(userRepository).existsByEmail(userSignupDto.getEmail());
        verify(storeRepository).existsByGstNumber(userSignupDto.getGstNumber());
        
        verify(encoder).encode(userSignupDto.getPassword());
        verify(userRepository).save(any(UserEntity.class));
        verify(storeRepository, times(2)).save(any(StoreEntity.class)); 
        verify(notificationService).sendOwnerWelcomeNotification(any(UserEntity.class), any(StoreEntity.class));
    }

    @Test
    void signup_UsernameAlreadyExists_ThrowsException() {
        when(userRepository.existsByUsername(userSignupDto.getUsername())).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class, () -> {
            userService.signup(userSignupDto);
        });

        verify(userRepository).existsByUsername(userSignupDto.getUsername());
        verify(userRepository, never()).existsByEmail(any());
        verify(storeRepository, never()).existsByGstNumber(any());
        verify(userRepository, never()).save(any());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void signup_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByUsername(userSignupDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(userSignupDto.getEmail())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> {
            userService.signup(userSignupDto);
        });

        verify(userRepository).existsByUsername(userSignupDto.getUsername());
        verify(userRepository).existsByEmail(userSignupDto.getEmail());
        verify(storeRepository, never()).existsByGstNumber(any());
        verify(userRepository, never()).save(any());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void signup_GstNumberAlreadyExists_ThrowsException() {
        when(userRepository.existsByUsername(userSignupDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(userSignupDto.getEmail())).thenReturn(false);
        when(storeRepository.existsByGstNumber(userSignupDto.getGstNumber())).thenReturn(true);

        assertThrows(GstNumberAlreadyExistsException.class, () -> {
            userService.signup(userSignupDto);
        });

        verify(userRepository).existsByUsername(userSignupDto.getUsername());
        verify(userRepository).existsByEmail(userSignupDto.getEmail());
        verify(storeRepository).existsByGstNumber(userSignupDto.getGstNumber());
        verify(userRepository, never()).save(any());
        verify(storeRepository, never()).save(any());
    }

    @Test
    void signup_NotificationFails_StillReturnsSuccess() {
        when(userRepository.existsByUsername(userSignupDto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(userSignupDto.getEmail())).thenReturn(false);
        when(storeRepository.existsByGstNumber(userSignupDto.getGstNumber())).thenReturn(false);
        
        when(modelMapper.map(userSignupDto, UserEntity.class)).thenReturn(userEntity);
        when(modelMapper.map(userSignupDto, StoreEntity.class)).thenReturn(storeEntity);
        
        when(encoder.encode(userSignupDto.getPassword())).thenReturn("encodedPassword");
        when(storeRepository.save(any(StoreEntity.class))).thenReturn(storeEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        
        doThrow(new RuntimeException("Email service down"))
        .when(notificationService).sendOwnerWelcomeNotification(any(), any());

        String result = userService.signup(userSignupDto);

        assertEquals("User registered successfully", result);
        verify(notificationService).sendOwnerWelcomeNotification(any(UserEntity.class), any(StoreEntity.class));
    }
}