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

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.ims.dto.BatchUpdateDto;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.BatchDoesNotBelongToUserException;
import com.ims.exception.BatchNotFoundException;
import com.ims.exception.EmailFailedException;
import com.ims.repository.BatchRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceUpdateBatchTest {

    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks private ProductService productService;

    private UserEntity user;
    private StoreEntity store;
    private ProductEntity product;
    private BatchEntity batch;
    private BatchUpdateDto dto;

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
        batch.setExpiryDate(LocalDate.of(2026, 12, 31));
        batch.setProduct(product);

        dto = new BatchUpdateDto(1L, "B001-UPDATED", 25.0, LocalDate.of(2027, 6, 30), 250.0);
    }

    @Test
    void updateBatch_success_updatesBatchAndProduct() throws Exception {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));

        String result = productService.updateBatch("owner1", dto);

        assertThat(result).isEqualTo("Batch updated successfully.");
        assertThat(batch.getBatchNumber()).isEqualTo("B001-UPDATED");
        assertThat(batch.getCurrentQuantity()).isEqualTo(25.0);
        assertThat(batch.getPurchasePrice()).isEqualTo(250.0);
        assertThat(batch.getExpiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        assertThat(product.getTotalQuantity()).isEqualTo(55.0);
        assertThat(product.getTotalPurchasePrice()).isEqualTo(550.0);
        verify(batchRepository).save(batch);
        verify(productRepository).save(product);
        verify(notificationService).sendBatchUpdateNotification(
                eq(user), eq(product), eq("B001"), eq(20.0), eq(200.0),
                eq(LocalDate.of(2026, 12, 31)), eq(dto));
    }

    @Test
    void updateBatch_batchNotFound_throwsBatchNotFoundException() {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BatchNotFoundException.class,
                () -> productService.updateBatch("owner1", dto));

        verify(batchRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void updateBatch_batchDoesNotBelongToStore_throwsBatchDoesNotBelongToUserException() {
        StoreEntity otherStore = new StoreEntity();
        otherStore.setId(99L);
        product.setStore(otherStore);

        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));

        assertThrows(BatchDoesNotBelongToUserException.class,
                () -> productService.updateBatch("owner1", dto));

        verify(batchRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void updateBatch_optimisticLockingFailure_throwsRuntimeException() {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenThrow(new ObjectOptimisticLockingFailureException(BatchEntity.class, 1L));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productService.updateBatch("owner1", dto));

        assertThat(ex.getMessage()).contains("modified by another user");
        verifyNoInteractions(notificationService);
    }

    @Test
    void updateBatch_emailFails_stillSavesBatchAndProduct() throws Exception {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));
        doThrow(new EmailFailedException("SMTP down"))
                .when(notificationService)
                .sendBatchUpdateNotification(any(), any(), any(), any(), any(), any(), any());

        String result = productService.updateBatch("owner1", dto);

        assertThat(result).isEqualTo("Batch updated successfully.");
        verify(batchRepository).save(batch);
        verify(productRepository).save(product);
    }

    @Test
    void updateBatch_quantityDecreased_reducesTotalQuantityCorrectly() throws Exception {
        dto = new BatchUpdateDto(1L, "B001-UPDATED", 10.0, LocalDate.of(2027, 6, 30), 200.0);

        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(batchRepository.findById(1L)).thenReturn(Optional.of(batch));

        productService.updateBatch("owner1", dto);

        assertThat(product.getTotalQuantity()).isEqualTo(40.0);
        verify(batchRepository).save(batch);
        verify(productRepository).save(product);
    }
}