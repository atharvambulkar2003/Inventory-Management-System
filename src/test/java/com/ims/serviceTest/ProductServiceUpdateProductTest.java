package com.ims.serviceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ims.dto.ProductEditDto;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailFailedException;
import com.ims.exception.ProductNameAlreadyExistsInStore;
import com.ims.exception.ProductNotFoundException;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceUpdateProductTest {

    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks private ProductService productService;

    private UserEntity user;
    private StoreEntity store;
    private ProductEntity existingProduct;
    private ProductEditDto dto;

    @BeforeEach
    void setUp() {
        store = new StoreEntity();
        store.setId(1L);

        user = new UserEntity();
        user.setUsername("owner1");
        user.setStore(store);

        existingProduct = new ProductEntity();
        existingProduct.setId(10L);
        existingProduct.setProductCode("P001");
        existingProduct.setProductName("OLD NAME");
        existingProduct.setCategory("Electronics");
        existingProduct.setMinStockLevel(5.0);
        existingProduct.setActive(true);
        existingProduct.setStore(store);

        dto = new ProductEditDto("P001", "New Name", "Clothing", 10.0);
    }

    @Test
    void updateProduct_success_savesAndReturnsMessage() throws Exception {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(existingProduct);
        when(productRepository.findByProductNameAndStoreAndActiveTrue(eq("NEW NAME"), any()))
                .thenReturn(null);

        String result = productService.updateProduct(dto, "owner1");

        assertThat(result).isEqualTo("Product Updated successfully");
        assertThat(existingProduct.getProductName()).isEqualTo("NEW NAME");
        assertThat(existingProduct.getCategory()).isEqualTo("Clothing");
        assertThat(existingProduct.getMinStockLevel()).isEqualTo(10.0);
        verify(productRepository).save(existingProduct);
        verify(notificationService).sendProductUpdateNotification(
                eq(user), eq("OLD NAME"), eq("Electronics"), eq(5.0), eq(dto));
    }

    @Test
    void updateProduct_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.updateProduct(dto, "ghost"));

        verifyNoInteractions(productRepository, notificationService);
    }

    @Test
    void updateProduct_storeNotFound_throwsStoreNotFoundException() {
        user.setStore(null);
        when(userRepository.findByUsername("owner1")).thenReturn(user);

        assertThrows(StoreNotFoundException.class,
                () -> productService.updateProduct(dto, "owner1"));

        verifyNoInteractions(productRepository, notificationService);
    }

    @Test
    void updateProduct_productNotFound_throwsProductNotFoundException() {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(null);

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateProduct(dto, "owner1"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_duplicateName_differentProduct_throwsProductNameAlreadyExists() {
        ProductEntity anotherProduct = new ProductEntity();
        anotherProduct.setId(99L);

        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(existingProduct);
        when(productRepository.findByProductNameAndStoreAndActiveTrue(eq("NEW NAME"), any()))
                .thenReturn(anotherProduct);

        assertThrows(ProductNameAlreadyExistsInStore.class,
                () -> productService.updateProduct(dto, "owner1"));

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_emailFails_stillSavesProduct() throws Exception {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(existingProduct);
        when(productRepository.findByProductNameAndStoreAndActiveTrue(eq("NEW NAME"), any()))
                .thenReturn(null);
        doThrow(new EmailFailedException("SMTP down"))
                .when(notificationService)
                .sendProductUpdateNotification(any(), any(), any(), anyDouble(), any()); // fix here

        String result = productService.updateProduct(dto, "owner1");

        assertThat(result).isEqualTo("Product Updated successfully");
        verify(productRepository).save(existingProduct);
    }

    @Test
    void updateProduct_sameNameSameProduct_doesNotThrowDuplicateException() {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(existingProduct);
        when(productRepository.findByProductNameAndStoreAndActiveTrue(eq("NEW NAME"), any()))
                .thenReturn(existingProduct);

        assertDoesNotThrow(() -> productService.updateProduct(dto, "owner1"));
        verify(productRepository).save(existingProduct);
    }
}