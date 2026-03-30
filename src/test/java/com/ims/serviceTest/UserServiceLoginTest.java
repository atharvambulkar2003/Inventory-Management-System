package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ims.dto.SigninResponseVO;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private UserEntity userEntity;
    private StoreEntity storeEntity;
    private SigninResponseVO signinResponseVO;
    private String username;

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

        signinResponseVO = new SigninResponseVO();
        signinResponseVO.setFullName("John Doe");
        signinResponseVO.setEmail("john@example.com");
        signinResponseVO.setRole("ROLE_OWNER");
        signinResponseVO.setActive(true);
        signinResponseVO.setStoreName("John's Store");
        signinResponseVO.setGstNumber("12ABCDE1234F1Z5");
    }

    @Test
    void getUserDetailsAfterLogin_Success() {
        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(modelMapper.map(userEntity, SigninResponseVO.class)).thenReturn(signinResponseVO);

        SigninResponseVO result = userService.getUserDetailsAfterLogin(username);

        assertNotNull(result);
        assertEquals("John Doe", result.getFullName());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("ROLE_OWNER", result.getRole());
        assertEquals(true, result.isActive());
        assertEquals("John's Store", result.getStoreName());
        assertEquals("12ABCDE1234F1Z5", result.getGstNumber());

        verify(userRepository).existsByUsername(username);
        verify(userRepository).findByUsername(username);
        verify(modelMapper).map(userEntity, SigninResponseVO.class);
        verify(modelMapper).map(userEntity.getStore(), signinResponseVO);
        verify(notificationService).sendLoginAlert(userEntity);
    }

    @Test
    void getUserDetailsAfterLogin_UsernameNotExists_ThrowsException() {
        when(userRepository.existsByUsername(username)).thenReturn(false);

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.getUserDetailsAfterLogin(username);
        });

        verify(userRepository).existsByUsername(username);
        verify(userRepository, never()).findByUsername(any());
        verify(notificationService, never()).sendLoginAlert(any());
    }

    @Test
    void getUserDetailsAfterLogin_NotificationFails_StillReturnsResponse() {
        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(modelMapper.map(userEntity, SigninResponseVO.class)).thenReturn(signinResponseVO);

        doThrow(new RuntimeException("Email service down"))
            .when(notificationService).sendLoginAlert(userEntity);

        SigninResponseVO result = userService.getUserDetailsAfterLogin(username);

        assertNotNull(result);
        assertEquals("John Doe", result.getFullName());
        assertEquals("john@example.com", result.getEmail());

        verify(userRepository).existsByUsername(username);
        verify(userRepository).findByUsername(username);
        verify(notificationService).sendLoginAlert(userEntity);
    }

    @Test
    void getUserDetailsAfterLogin_InactiveUser_ReturnsInactiveStatus() {
        userEntity.setActive(false);
        signinResponseVO.setActive(false);

        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(modelMapper.map(userEntity, SigninResponseVO.class)).thenReturn(signinResponseVO);

        SigninResponseVO result = userService.getUserDetailsAfterLogin(username);

        assertNotNull(result);
        assertEquals(false, result.isActive());

        verify(userRepository).existsByUsername(username);
        verify(userRepository).findByUsername(username);
        verify(notificationService).sendLoginAlert(userEntity);
    }

    @Test
    void getUserDetailsAfterLogin_UserWithDifferentRole_Success() {
        userEntity.setRole("ROLE_MANAGER");
        signinResponseVO.setRole("ROLE_MANAGER");

        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(userRepository.findByUsername(username)).thenReturn(userEntity);
        when(modelMapper.map(userEntity, SigninResponseVO.class)).thenReturn(signinResponseVO);

        SigninResponseVO result = userService.getUserDetailsAfterLogin(username);

        assertNotNull(result);
        assertEquals("ROLE_MANAGER", result.getRole());

        verify(userRepository).existsByUsername(username);
        verify(userRepository).findByUsername(username);
    }
}