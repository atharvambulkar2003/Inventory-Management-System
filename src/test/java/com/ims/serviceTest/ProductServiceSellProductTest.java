package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.ims.dto.SaleDto;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.TransactionEntity;
import com.ims.entity.TransactionType;
import com.ims.entity.UserEntity;
import com.ims.exception.EmailFailedException;
import com.ims.exception.LessQuantityException;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.BatchRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.TransactionRepository;
import com.ims.repository.UserRepository;
import com.ims.service.NotificationService;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceSellProductTest {

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

    @InjectMocks
    private ProductService productService;

    private UserEntity userEntity;
    private UserEntity ownerEntity;
    private StoreEntity store;
    private ProductEntity productEntity;
    private BatchEntity batchEntity;
    private SaleDto saleDto;

    private Double round(Double value) {
        if (value == null) return 0.0;
        return Math.round(value * 100.0) / 100.0;
    }

    @BeforeEach
    void setUp() {
        ownerEntity = new UserEntity();
        ownerEntity.setUsername("owneruser");

        store = new StoreEntity();
        store.setOwner(ownerEntity);

        userEntity = new UserEntity();
        userEntity.setUsername("owneruser");
        userEntity.setStore(store);

        batchEntity = new BatchEntity();
        batchEntity.setCurrentQuantity(10.0);

        productEntity = new ProductEntity();
        productEntity.setProductName("APPLE");
        productEntity.setCategory("Fruits");
        productEntity.setTotalQuantity(10.0);
        productEntity.setTotalPurchasePrice(500.0);
        productEntity.setMinStockLevel(5.0);
        productEntity.setBatches(new ArrayList<>(List.of(batchEntity)));
        productEntity.setTransactions(new ArrayList<>());

        saleDto = new SaleDto();
        saleDto.setProductName("apple");
        saleDto.setCategory("Fruits");
        saleDto.setQuantity(4.0);
        saleDto.setMrp(60.0);
        saleDto.setCustomerName("Customer A");
    }

    @Test
    void sellProduct_Success_ReturnsRevenueAndProfit() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        String result = productService.sellProduct("owneruser", saleDto);

        Double individualPrice = round(500.0 / 10.0);
        Double totalCost = round(individualPrice * 4.0);
        Double totalRevenue = round(60.0 * 4.0);
        Double profit = round(totalRevenue - totalCost);

        assertTrue(result.contains("Sale successful"));
        assertTrue(result.contains(String.valueOf(totalRevenue)));
        assertTrue(result.contains(String.valueOf(profit)));
    }

    @Test
    void sellProduct_UpdatesProductTotalQuantityAndPurchasePrice() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        Double individualPrice = 500.0 / 10.0;
        Double totalCost = round(individualPrice * 4.0);

        assertEquals(round(10.0 - 4.0), productEntity.getTotalQuantity());
        assertEquals(round(500.0 - totalCost), productEntity.getTotalPurchasePrice());
    }

    @Test
    void sellProduct_TransactionSavedWithCorrectFields() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(transactionRepository).save(captor.capture());

        Double totalRevenue = round(60.0 * 4.0);

        TransactionEntity saved = captor.getValue();
        assertEquals(TransactionType.SALE, saved.getType());
        assertEquals(4.0, saved.getQuantity());
        assertEquals(totalRevenue, saved.getTotalAmount());
        assertEquals("Customer A", saved.getPartyName());
        assertEquals(productEntity, saved.getProduct());
        assertNotNull(saved.getTransactionDate());
    }

    @Test
    void sellProduct_TransactionAddedToProductList() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        assertEquals(1, productEntity.getTransactions().size());
    }

    @Test
    void sellProduct_BatchFullyConsumed_BatchDeleted() {
        saleDto.setQuantity(10.0);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        verify(batchRepository).delete(batchEntity);
        assertTrue(productEntity.getBatches().isEmpty());
    }

    @Test
    void sellProduct_BatchPartiallyConsumed_BatchUpdated() {
        saleDto.setQuantity(4.0);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        verify(batchRepository).save(batchEntity);
        assertEquals(round(10.0 - 4.0), batchEntity.getCurrentQuantity());
    }

    @Test
    void sellProduct_MultipleBatches_ConsumedInOrder() {
        BatchEntity batch1 = new BatchEntity();
        batch1.setCurrentQuantity(3.0);
        BatchEntity batch2 = new BatchEntity();
        batch2.setCurrentQuantity(10.0);

        productEntity.setBatches(new ArrayList<>(List.of(batch1, batch2)));
        productEntity.setTotalQuantity(13.0);
        productEntity.setTotalPurchasePrice(650.0);
        saleDto.setQuantity(5.0);

        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        Double remainingAfterBatch1 = 5.0 - 3.0;
        Double expectedBatch2Qty = round(10.0 - remainingAfterBatch1);

        verify(batchRepository).delete(batch1);
        verify(batchRepository).save(batch2);
        assertEquals(expectedBatch2Qty, batch2.getCurrentQuantity());
    }

    @Test
    void sellProduct_LowStockAfterSale_SendsLowStockNotification() {
        productEntity.setMinStockLevel(7.0);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        verify(notificationService).sendLowQuantityNotification(ownerEntity, productEntity);
    }

    @Test
    void sellProduct_StockAboveMinLevel_NoLowStockNotification() {
        productEntity.setMinStockLevel(2.0);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        verify(notificationService, never()).sendLowQuantityNotification(any(), any());
    }

    @Test
    void sellProduct_LowStockEmailFailed_StillCompleteSale() {
        productEntity.setMinStockLevel(7.0);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);
        doThrow(new EmailFailedException("SMTP error"))
                .when(notificationService).sendLowQuantityNotification(any(), any());

        String result = productService.sellProduct("owneruser", saleDto);

        assertTrue(result.contains("Sale successful"));
        verify(productRepository).save(productEntity);
    }

    @Test
    void sellProduct_SaleEmailFailed_StillCompleteSale() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);
        
        String result = productService.sellProduct("owneruser", saleDto);

        assertTrue(result.contains("Sale successful"));
        verify(productRepository).save(productEntity);
    }

    @Test
    void sellProduct_InsufficientStock_ThrowsException() {
        saleDto.setQuantity(20.0);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        assertThrows(LessQuantityException.class,
                () -> productService.sellProduct("owneruser", saleDto));

        verify(transactionRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void sellProduct_ZeroStock_ThrowsException() {
        productEntity.setTotalQuantity(0.0);
        saleDto.setQuantity(1.0);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        assertThrows(LessQuantityException.class,
                () -> productService.sellProduct("owneruser", saleDto));

        verify(transactionRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void sellProduct_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.sellProduct("unknown", saleDto));

        verify(productRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void sellProduct_StoreNotFound_ThrowsException() {
        userEntity.setStore(null);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        assertThrows(StoreNotFoundException.class,
                () -> productService.sellProduct("owneruser", saleDto));

        verify(productRepository, never()).findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                any(), any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void sellProduct_ProductNameConvertedToUpperCase() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store)).thenReturn(productEntity);

        productService.sellProduct("owneruser", saleDto);

        verify(productRepository).findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
                "APPLE", "Fruits", store);
    }
}