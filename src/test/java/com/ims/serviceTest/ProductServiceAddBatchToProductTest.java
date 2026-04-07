package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.ims.dto.AddBatchToProductDto;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.TransactionEntity;
import com.ims.entity.TransactionType;
import com.ims.entity.UserEntity;
import com.ims.exception.BatchNumberExistsException;
import com.ims.exception.EmailFailedException;
import com.ims.exception.ProductNotFoundException;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.BatchRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.TransactionRepository;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceAddBatchToProductTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductService productService;

    private UserEntity userEntity;
    private UserEntity ownerEntity;
    private StoreEntity store;
    private ProductEntity productEntity;
    private BatchEntity batchEntity;
    private AddBatchToProductDto dto;

    @BeforeEach
    void setUp() {
        ownerEntity = new UserEntity();
        ownerEntity.setUsername("owneruser");

        store = new StoreEntity();
        store.setOwner(ownerEntity);

        userEntity = new UserEntity();
        userEntity.setUsername("owneruser");
        userEntity.setStore(store);

        productEntity = new ProductEntity();
        productEntity.setProductName("APPLE");
        productEntity.setTotalQuantity(10.0);
        productEntity.setTotalPurchasePrice(500.0);
        productEntity.setTransactions(new ArrayList<>());

        batchEntity = new BatchEntity();

        dto = new AddBatchToProductDto();
        dto.setProductName("apple");
        dto.setBatchNumber("B001");
        dto.setCurrentQuantity(5.0);
        dto.setPurchasePrice(250.0);
        dto.setPartyName("Supplier A");
        dto.setCategory("Fruits");
        dto.setExpiryDate(LocalDate.now().plusMonths(3));
    }

    @Test
    void addBatchToProduct_Success_ReturnMessage() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.existsByBatchNumberAndProductStore("B001", store)).thenReturn(false);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(productEntity);
        when(modelMapper.map(dto, BatchEntity.class)).thenReturn(batchEntity);

        String result = productService.addBatchToProduct("owneruser", dto);

        assertEquals("Purchase done Successfully", result);  // fixed
        verify(batchRepository).save(batchEntity);
        verify(transactionRepository).save(any(TransactionEntity.class));
        verify(productRepository).save(productEntity);
        verify(notificationService).sendBatchAdditionNotification(ownerEntity, productEntity, batchEntity);
    }

    @Test
    void addBatchToProduct_UpdatesTotalQuantityAndPurchasePrice() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.existsByBatchNumberAndProductStore("B001", store)).thenReturn(false);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(productEntity);
        when(modelMapper.map(dto, BatchEntity.class)).thenReturn(batchEntity);

        productService.addBatchToProduct("owneruser", dto);

        assertEquals(15.0, productEntity.getTotalQuantity());
        assertEquals(1750.0, productEntity.getTotalPurchasePrice());
        
        
    }

    @Test
    void addBatchToProduct_TransactionSavedWithCorrectFields() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.existsByBatchNumberAndProductStore("B001", store)).thenReturn(false);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(productEntity);
        when(modelMapper.map(dto, BatchEntity.class)).thenReturn(batchEntity);

        productService.addBatchToProduct("owneruser", dto);

        ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        TransactionEntity savedTransaction = transactionCaptor.getValue();
        assertEquals(TransactionType.PURCHASE, savedTransaction.getType());
        assertEquals(5.0, savedTransaction.getQuantity());
        assertEquals(1250.0, savedTransaction.getTotalAmount());
        assertEquals("Supplier A", savedTransaction.getPartyName());
        assertEquals(productEntity, savedTransaction.getProduct());
        assertNotNull(savedTransaction.getTransactionDate());
        
        
    }

    @Test
    void addBatchToProduct_TransactionAddedToProductList() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.existsByBatchNumberAndProductStore("B001", store)).thenReturn(false);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(productEntity);
        when(modelMapper.map(dto, BatchEntity.class)).thenReturn(batchEntity);

        productService.addBatchToProduct("owneruser", dto);

        assertEquals(1, productEntity.getTransactions().size());
    }

    @Test
    void addBatchToProduct_ProductNameConvertedToUpperCase() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.existsByBatchNumberAndProductStore("B001", store)).thenReturn(false);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(productEntity);
        when(modelMapper.map(dto, BatchEntity.class)).thenReturn(batchEntity);

        productService.addBatchToProduct("owneruser", dto);

        verify(productRepository).findByProductNameAndStoreAndActiveTrue("APPLE", store);
    }

    @Test
    void addBatchToProduct_EmailFailed_StillSavesBatch() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.existsByBatchNumberAndProductStore("B001", store)).thenReturn(false);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(productEntity);
        when(modelMapper.map(dto, BatchEntity.class)).thenReturn(batchEntity);
        doThrow(new EmailFailedException("SMTP error"))
                .when(notificationService).sendBatchAdditionNotification(any(), any(), any());

        String result = productService.addBatchToProduct("owneruser", dto);

        assertEquals("Purchase done Successfully", result);  // fixed
        verify(batchRepository).save(batchEntity);
        verify(productRepository).save(productEntity);
    }

    @Test
    void addBatchToProduct_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.addBatchToProduct("unknown", dto));

        verify(batchRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void addBatchToProduct_StoreNotFound_ThrowsException() {
        userEntity.setStore(null);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        assertThrows(StoreNotFoundException.class,
                () -> productService.addBatchToProduct("owneruser", dto));

        verify(batchRepository, never()).save(any());
    }

    @Test
    void addBatchToProduct_BatchNumberAlreadyExists_ThrowsException() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.existsByBatchNumberAndProductStore("B001", store)).thenReturn(true);

        assertThrows(BatchNumberExistsException.class,
                () -> productService.addBatchToProduct("owneruser", dto));

        verify(batchRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void addBatchToProduct_ProductNotFound_ThrowsException() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.existsByBatchNumberAndProductStore("B001", store)).thenReturn(false);
        when(productRepository.findByProductNameAndStoreAndActiveTrue("APPLE", store)).thenReturn(null);

        assertThrows(ProductNotFoundException.class,
                () -> productService.addBatchToProduct("owneruser", dto));

        verify(batchRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}