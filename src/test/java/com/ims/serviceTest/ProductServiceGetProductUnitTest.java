package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.ProductNotFoundException;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceGetProductUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private UserEntity userEntity;
    private StoreEntity store;
    private ProductEntity productEntity;

    @BeforeEach
    void setUp() {
        store = new StoreEntity();
        store.setStoreName("TestStore");

        userEntity = new UserEntity();
        userEntity.setUsername("owneruser");
        userEntity.setStore(store);

        productEntity = new ProductEntity();
        productEntity.setProductName("APPLE");
        productEntity.setDefaultUnits("kg");
    }

    @Test
    void getProductUnit_Success_ReturnsUnit() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(productEntity);

        String result = productService.getProductUnit("owneruser", "apple");

        assertEquals("kg", result);
        verify(productRepository).findByProductNameAndStoreAndActiveTrue("APPLE", store);
    }

    @Test
    void getProductUnit_ProductNameConvertedToUpperCase() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(productEntity);

        productService.getProductUnit("owneruser", "apple");

        verify(productRepository).findByProductNameAndStoreAndActiveTrue("APPLE", store);
    }

    @Test
    void getProductUnit_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.getProductUnit("unknown", "apple"));

        verify(productRepository, never()).findByProductNameAndStoreAndActiveTrue(any(), any());
    }

    @Test
    void getProductUnit_StoreNotFound_ThrowsException() {
        userEntity.setStore(null);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        assertThrows(StoreNotFoundException.class,
                () -> productService.getProductUnit("owneruser", "apple"));

        verify(productRepository, never()).findByProductNameAndStoreAndActiveTrue(any(), any());
    }

    @Test
    void getProductUnit_ProductNotFound_ThrowsException() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("UNKNOWN", store)).thenReturn(null);

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductUnit("owneruser", "unknown"));
    }
}