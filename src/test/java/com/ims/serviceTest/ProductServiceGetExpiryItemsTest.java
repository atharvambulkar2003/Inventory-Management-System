package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.ims.dto.BatchExpiryVO;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.BatchRepository;
import com.ims.repository.UserRepository;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceGetExpiryItemsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductService productService;

    private UserEntity userEntity;
    private UserEntity ownerEntity;
    private StoreEntity store;
    private BatchEntity batchEntity;
    private ProductEntity productEntity;
    private BatchExpiryVO batchExpiryVO;

    @BeforeEach
    void setUp() {
        productEntity = new ProductEntity();
        productEntity.setProductName("APPLE");
        productEntity.setProductCode("P001");
        productEntity.setDefaultUnits("kg");

        ownerEntity = new UserEntity();
        ownerEntity.setUsername("owneruser");

        store = new StoreEntity();
        store.setOwner(ownerEntity);

        userEntity = new UserEntity();
        userEntity.setUsername("owneruser");
        userEntity.setStore(store);

        batchEntity = new BatchEntity();
        batchEntity.setProduct(productEntity);

        batchExpiryVO = new BatchExpiryVO();
    }

    @Test
    void getExpiryItems_Success_ReturnsExpiryList() {
        List<BatchEntity> batches = List.of(batchEntity);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.findExpiringBatches(
                eq("owneruser"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(batches);
        when(modelMapper.map(batchEntity, BatchExpiryVO.class)).thenReturn(batchExpiryVO);

        List<BatchExpiryVO> result = productService.getExpiryItems("owneruser");

        assertEquals(1, result.size());
        assertEquals("APPLE", result.get(0).getProductName());
        assertEquals("P001", result.get(0).getProductCode());
        assertEquals("kg", result.get(0).getDefaultUnits());
        verify(batchRepository).findExpiringBatches(eq("owneruser"), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getExpiryItems_EmptyBatches_ReturnsEmptyList() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.findExpiringBatches(
                eq("owneruser"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        List<BatchExpiryVO> result = productService.getExpiryItems("owneruser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getExpiryItems_MultipleBatches_ReturnsAllMapped() {
        ProductEntity productEntity2 = new ProductEntity();
        productEntity2.setProductName("BANANA");
        productEntity2.setProductCode("P002");
        productEntity2.setDefaultUnits("dozen");

        BatchEntity batchEntity2 = new BatchEntity();
        batchEntity2.setProduct(productEntity2);

        BatchExpiryVO batchExpiryVO2 = new BatchExpiryVO();

        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.findExpiringBatches(
                eq("owneruser"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(batchEntity, batchEntity2));
        when(modelMapper.map(batchEntity, BatchExpiryVO.class)).thenReturn(batchExpiryVO);
        when(modelMapper.map(batchEntity2, BatchExpiryVO.class)).thenReturn(batchExpiryVO2);

        List<BatchExpiryVO> result = productService.getExpiryItems("owneruser");

        assertEquals(2, result.size());
        assertEquals("BANANA", result.get(1).getProductName());
        assertEquals("P002", result.get(1).getProductCode());
        assertEquals("dozen", result.get(1).getDefaultUnits());
    }

    @Test
    void getExpiryItems_DateRangeIsTodayToThirtyDays() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(batchRepository.findExpiringBatches(
                eq("owneruser"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        productService.getExpiryItems("owneruser");

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(batchRepository).findExpiringBatches(eq("owneruser"), fromCaptor.capture(), toCaptor.capture());

        assertEquals(LocalDate.now(), fromCaptor.getValue());
        assertEquals(LocalDate.now().plusDays(30), toCaptor.getValue());
    }

    @Test
    void getExpiryItems_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.getExpiryItems("unknown"));

        verify(batchRepository, never()).findExpiringBatches(any(), any(), any());
    }
}