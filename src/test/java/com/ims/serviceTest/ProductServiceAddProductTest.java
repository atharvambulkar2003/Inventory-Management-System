package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.ims.dto.ProductDto;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailFailedException;
import com.ims.exception.ProductCodeAlreadyExistsInStoreException;
import com.ims.exception.ProductNameAlreadyExistsInStore;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceAddProductTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductService productService;

    private UserEntity userEntity;
    private StoreEntity store;
    private ProductDto productDto;
    private ProductEntity productEntity;

    @BeforeEach
    void setUp() {
        store = new StoreEntity();
        store.setProducts(new ArrayList<>());

        userEntity = new UserEntity();
        userEntity.setUsername("owneruser");
        userEntity.setEmail("owner@example.com");
        userEntity.setStore(store);

        productDto = new ProductDto();
        productDto.setProductCode("P001");
        productDto.setProductName("Apple");
        productDto.setCategory("Fruits");
        productDto.setMinStockLevel(10.0);
        productDto.setDefaultUnits("kg");

        productEntity = new ProductEntity();
        productEntity.setProductCode("P001");
        productEntity.setProductName("APPLE");
        productEntity.setCategory("Fruits");
        productEntity.setMinStockLevel(10.0);
        productEntity.setDefaultUnits("kg");
    }

    @Test
    void addProduct_Success() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.existsByProductCodeAndStoreAndActiveTrue("P001", store)).thenReturn(false);
        when(productRepository.existsByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(false);
        when(modelMapper.map(productDto, ProductEntity.class)).thenReturn(productEntity);

        String result = productService.addProduct(productDto, "owneruser");

        assertEquals("Product Saved successfully", result);
        assertEquals(0.0, productEntity.getTotalQuantity());
        assertEquals(0.0, productEntity.getTotalPurchasePrice());
        assertEquals(store, productEntity.getStore());
        assertTrue(store.getProducts().contains(productEntity));
        verify(productRepository).save(productEntity);
        verify(notificationService).sendProductAdditionNotification(userEntity, productEntity);
    }

    @Test
    void addProduct_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.addProduct(productDto, "unknown"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void addProduct_ProductCodeAlreadyExists_ThrowsException() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.existsByProductCodeAndStoreAndActiveTrue("P001", store)).thenReturn(true);

        assertThrows(ProductCodeAlreadyExistsInStoreException.class,
                () -> productService.addProduct(productDto, "owneruser"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void addProduct_ProductNameAlreadyExists_ThrowsException() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.existsByProductCodeAndStoreAndActiveTrue("P001", store)).thenReturn(false);
        when(productRepository.existsByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(true);

        assertThrows(ProductNameAlreadyExistsInStore.class,
                () -> productService.addProduct(productDto, "owneruser"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void addProduct_EmailFailedException_StillSavesProduct() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.existsByProductCodeAndStoreAndActiveTrue("P001", store)).thenReturn(false);
        when(productRepository.existsByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(false);
        when(modelMapper.map(productDto, ProductEntity.class)).thenReturn(productEntity);
        doThrow(new EmailFailedException("SMTP error"))
                .when(notificationService).sendProductAdditionNotification(any(), any());

        String result = productService.addProduct(productDto, "owneruser");

        assertEquals("Product Saved successfully", result);
        verify(productRepository).save(productEntity);
    }

    @Test
    void addProduct_ProductNameStoredAsUpperCase() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.existsByProductCodeAndStoreAndActiveTrue("P001", store)).thenReturn(false);
        when(productRepository.existsByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(false);
        when(modelMapper.map(productDto, ProductEntity.class)).thenReturn(productEntity);

        productService.addProduct(productDto, "owneruser");

        assertEquals("APPLE", productEntity.getProductName());
    }
}