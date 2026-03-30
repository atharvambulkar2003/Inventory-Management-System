package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceGetProductsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private UserEntity userEntity;
    private StoreEntity store;

    @BeforeEach
    void setUp() {
        store = new StoreEntity();

        userEntity = new UserEntity();
        userEntity.setUsername("owneruser");
        userEntity.setStore(store);
    }

    @Test
    void getProducts_Success_ReturnsProductNames() {
        List<String> products = List.of("APPLE", "BANANA", "MANGO");
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findProductsByStoreAndCategoryAndActiveTrue(store, "Fruits")).thenReturn(products);

        List<String> result = productService.getProducts("owneruser", "Fruits");

        assertEquals(3, result.size());
        assertEquals(products, result);
        verify(productRepository).findProductsByStoreAndCategoryAndActiveTrue(store, "Fruits");
    }

    @Test
    void getProducts_EmptyList_ReturnsEmptyList() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findProductsByStoreAndCategoryAndActiveTrue(store, "Electronics")).thenReturn(List.of());

        List<String> result = productService.getProducts("owneruser", "Electronics");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getProducts_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.getProducts("unknown", "Fruits"));

        verify(productRepository, never()).findProductsByStoreAndCategoryAndActiveTrue(any(), any());
    }

    @Test
    void getProducts_StoreNotFound_ThrowsException() {
        userEntity.setStore(null);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        assertThrows(StoreNotFoundException.class,
                () -> productService.getProducts("owneruser", "Fruits"));

        verify(productRepository, never()).findProductsByStoreAndCategoryAndActiveTrue(any(), any());
    }
}