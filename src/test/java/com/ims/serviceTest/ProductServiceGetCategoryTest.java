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
class ProductServiceGetCategoryTest {

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
    void getCategory_Success_ReturnsCategories() {
        List<String> categories = List.of("Fruits", "Vegetables", "Dairy");
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findDistinctCategoriesByStoreAndActiveTrue(store)).thenReturn(categories);

        List<String> result = productService.getCategory("owneruser");

        assertEquals(3, result.size());
        assertEquals(categories, result);
        verify(productRepository).findDistinctCategoriesByStoreAndActiveTrue(store);
    }

    @Test
    void getCategory_EmptyCategories_ReturnsEmptyList() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findDistinctCategoriesByStoreAndActiveTrue(store)).thenReturn(List.of());

        List<String> result = productService.getCategory("owneruser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCategory_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.getCategory("unknown"));

        verify(productRepository, never()).findDistinctCategoriesByStoreAndActiveTrue(any());
    }

    @Test
    void getCategory_StoreNotFound_ThrowsException() {
        userEntity.setStore(null);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        assertThrows(StoreNotFoundException.class,
                () -> productService.getCategory("owneruser"));

        verify(productRepository, never()).findDistinctCategoriesByStoreAndActiveTrue(any());
    }
}