package com.ims.serviceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailFailedException;
import com.ims.exception.ProductNotFoundException;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.BatchRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceDeleteProductTest {

    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks private ProductService productService;

    private UserEntity user;
    private StoreEntity store;
    private ProductEntity existingProduct;

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
        existingProduct.setProductName("PRODUCT ONE");
        existingProduct.setActive(true);
        existingProduct.setTotalPurchasePrice(500.0);
        existingProduct.setTotalQuantity(20.0);
        existingProduct.setStore(store);
        existingProduct.setBatches(new ArrayList<>());
    }

    @Test
    void deleteProduct_success_noBatches_deactivatesProduct() throws Exception {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(existingProduct);

        String result = productService.deleteProduct("P001", "owner1");

        assertThat(result).isEqualTo("Product Deactivated successfully");
        assertThat(existingProduct.isActive()).isFalse();
        assertThat(existingProduct.getTotalPurchasePrice()).isEqualTo(0.0);
        assertThat(existingProduct.getTotalQuantity()).isEqualTo(0.0);
        verify(batchRepository, never()).deleteAll(any());
        verify(productRepository).save(existingProduct);
        verify(notificationService).sendProductDeactivationNotification(user, "PRODUCT ONE");
    }

    @Test
    void deleteProduct_success_withBatches_deletesBatchesAndDeactivates() throws Exception {
        BatchEntity batch1 = new BatchEntity();
        BatchEntity batch2 = new BatchEntity();
        existingProduct.setBatches(new ArrayList<>(List.of(batch1, batch2)));

        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(existingProduct);

        String result = productService.deleteProduct("P001", "owner1");

        assertThat(result).isEqualTo("Product Deactivated successfully");
        assertThat(existingProduct.isActive()).isFalse();
        assertThat(existingProduct.getBatches()).isEmpty();
        verify(batchRepository).deleteAll(any(List.class)); // just verify it was called
        verify(productRepository).save(existingProduct);
        verify(notificationService).sendProductDeactivationNotification(user, "PRODUCT ONE");
    }
    
    @Test
    void deleteProduct_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.deleteProduct("P001", "ghost"));

        verifyNoInteractions(productRepository, batchRepository, notificationService);
    }

    @Test
    void deleteProduct_storeNotFound_throwsStoreNotFoundException() {
        user.setStore(null);
        when(userRepository.findByUsername("owner1")).thenReturn(user);

        assertThrows(StoreNotFoundException.class,
                () -> productService.deleteProduct("P001", "owner1"));

        verifyNoInteractions(productRepository, batchRepository, notificationService);
    }

    @Test
    void deleteProduct_productNotFound_throwsProductNotFoundException() {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(null);

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct("P001", "owner1"));

        verify(productRepository, never()).save(any());
        verifyNoInteractions(batchRepository, notificationService);
    }

    @Test
    void deleteProduct_emailFails_stillDeactivatesProduct() throws Exception {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(productRepository.findByProductCodeAndStoreAndActiveTrue(eq("P001"), any()))
                .thenReturn(existingProduct);
        doThrow(new EmailFailedException("SMTP down"))
                .when(notificationService)
                .sendProductDeactivationNotification(any(), any());

        String result = productService.deleteProduct("P001", "owner1");

        assertThat(result).isEqualTo("Product Deactivated successfully");
        assertThat(existingProduct.isActive()).isFalse();
        verify(productRepository).save(existingProduct);
    }
}