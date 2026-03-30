package com.ims.serviceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.BatchDoesNotBelongToUserException;
import com.ims.exception.BatchNotFoundException;
import com.ims.exception.EmailFailedException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.BatchRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceDeleteBatchTest {

    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks private ProductService productService;

    private UserEntity user;
    private StoreEntity store;
    private ProductEntity product;
    private BatchEntity batch;

    @BeforeEach
    void setUp() {
        store = new StoreEntity();
        store.setId(1L);

        user = new UserEntity();
        user.setUsername("owner1");
        user.setStore(store);

        product = new ProductEntity();
        product.setId(10L);
        product.setProductName("PRODUCT ONE");
        product.setTotalQuantity(50.0);
        product.setTotalPurchasePrice(500.0);
        product.setStore(store);

        batch = new BatchEntity();
        batch.setId(1L);
        batch.setBatchNumber("B001");
        batch.setCurrentQuantity(20.0);
        batch.setPurchasePrice(200.0);
        batch.setProduct(product);

        product.setBatches(new ArrayList<>(List.of(batch)));
    }

    @Test
    void deleteBatch_success_updatesTotalsAndDeletesBatch() throws Exception {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));

        String result = productService.deleteBatch("owner1", 1L);

        assertThat(result).isEqualTo("Batch B001 deleted successfully.");
        assertThat(product.getTotalQuantity()).isEqualTo(30.0);
        assertThat(product.getTotalPurchasePrice()).isEqualTo(300.0);
        assertThat(product.getBatches()).doesNotContain(batch);
        verify(batchRepository).delete(batch);
        verify(productRepository).save(product);
        verify(notificationService).sendBatchDeletionNotification(user, "PRODUCT ONE", "B001", product);
    }

    @Test
    void deleteBatch_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.deleteBatch("ghost", 1L));

        verifyNoInteractions(batchRepository, productRepository, notificationService);
    }

    @Test
    void deleteBatch_batchNotFound_throwsBatchNotFoundException() {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BatchNotFoundException.class,
                () -> productService.deleteBatch("owner1", 1L));

        verify(batchRepository, never()).delete(any());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void deleteBatch_batchDoesNotBelongToStore_throwsBatchDoesNotBelongToUserException() {
        StoreEntity otherStore = new StoreEntity();
        otherStore.setId(99L);
        product.setStore(otherStore);

        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));

        assertThrows(BatchDoesNotBelongToUserException.class,
                () -> productService.deleteBatch("owner1", 1L));

        verify(batchRepository, never()).delete(any());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void deleteBatch_optimisticLockingFailure_throwsRuntimeException() {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        doThrow(new ObjectOptimisticLockingFailureException(BatchEntity.class, 1L))
                .when(batchRepository).delete(any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productService.deleteBatch("owner1", 1L));

        assertThat(ex.getMessage()).contains("modified by another user");
        verifyNoInteractions(notificationService);
    }

    @Test
    void deleteBatch_emailFails_stillDeletesBatchAndSavesProduct() throws Exception {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        doThrow(new EmailFailedException("SMTP down"))
                .when(notificationService)
                .sendBatchDeletionNotification(any(), any(), any(), any());

        String result = productService.deleteBatch("owner1", 1L);

        assertThat(result).isEqualTo("Batch B001 deleted successfully.");
        verify(batchRepository).delete(batch);
        verify(productRepository).save(product);
    }
}