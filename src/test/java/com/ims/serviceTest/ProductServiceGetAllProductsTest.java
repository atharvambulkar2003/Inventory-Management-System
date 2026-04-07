package com.ims.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.ims.dto.BatchVO;
import com.ims.dto.ProductVO;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.UserRepository;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceGetAllProductsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductService productService;

    private UserEntity userEntity;
    private StoreEntity store;
    private ProductEntity activeProduct;
    private ProductEntity inactiveProduct;
    private BatchEntity batchEntity;
    private ProductVO productVO;
    private BatchVO batchVO;

    @BeforeEach
    void setUp() {
        batchEntity = new BatchEntity();
        batchEntity.setCurrentQuantity(10.0);

        activeProduct = new ProductEntity();
        activeProduct.setProductName("APPLE");
        activeProduct.setCategory("Fruits");
        activeProduct.setTotalQuantity(10.0);
        activeProduct.setTotalPurchasePrice(500.0);
        activeProduct.setActive(true);
        activeProduct.setBatches(new ArrayList<>(List.of(batchEntity)));

        inactiveProduct = new ProductEntity();
        inactiveProduct.setProductName("MANGO");
        inactiveProduct.setCategory("Fruits");
        inactiveProduct.setTotalQuantity(5.0);
        inactiveProduct.setTotalPurchasePrice(200.0);
        inactiveProduct.setActive(false);
        inactiveProduct.setBatches(new ArrayList<>());

        store = new StoreEntity();
        store.setProducts(new ArrayList<>(List.of(activeProduct, inactiveProduct)));

        userEntity = new UserEntity();
        userEntity.setUsername("owneruser");
        userEntity.setStore(store);

        batchVO = new BatchVO();
        batchVO.setCurrentQuantity(10.0);

        productVO = new ProductVO();
        productVO.setProductName("APPLE");
        productVO.setCategory("Fruits");
        productVO.setTotalQuantity(10.0);
        productVO.setTotalPurchasePrice(500.0);
    }

    @Test
    void getAllProducts_Success_ReturnsOnlyActiveProducts() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(modelMapper.map(activeProduct, ProductVO.class)).thenReturn(productVO);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertEquals(1, result.size());
        assertEquals("APPLE", result.get(0).getProductName());
    }

    @Test
    void getAllProducts_InactiveProductsExcluded() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(modelMapper.map(activeProduct, ProductVO.class)).thenReturn(productVO);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertTrue(result.stream().noneMatch(p -> "MANGO".equals(p.getProductName())));
    }

    @Test
    void getAllProducts_BatchesMappedAndSetOnProductVO() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(modelMapper.map(activeProduct, ProductVO.class)).thenReturn(productVO);
        when(modelMapper.map(batchEntity, BatchVO.class)).thenReturn(batchVO);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertEquals(1, result.get(0).getBatches().size());
        assertEquals(batchVO, result.get(0).getBatches().get(0));
    }

    @Test
    void getAllProducts_MultipleBatchesMapped() {
        BatchEntity batch2 = new BatchEntity();
        batch2.setCurrentQuantity(5.0);
        BatchVO batchVO2 = new BatchVO();
        batchVO2.setCurrentQuantity(5.0);

        activeProduct.setBatches(new ArrayList<>(List.of(batchEntity, batch2)));

        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(modelMapper.map(activeProduct, ProductVO.class)).thenReturn(productVO);
        when(modelMapper.map(batchEntity, BatchVO.class)).thenReturn(batchVO);
        when(modelMapper.map(batch2, BatchVO.class)).thenReturn(batchVO2);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertEquals(2, result.get(0).getBatches().size());
    }

    @Test
    void getAllProducts_EmptyProductList_ReturnsEmptyList() {
        store.setProducts(new ArrayList<>());
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllProducts_AllProductsInactive_ReturnsEmptyList() {
        store.setProducts(new ArrayList<>(List.of(inactiveProduct)));
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllProducts_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.getAllProducts("unknown"));

        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void getAllProducts_StoreNotFound_ThrowsException() {
        userEntity.setStore(null);
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        assertThrows(StoreNotFoundException.class,
                () -> productService.getAllProducts("owneruser"));

        verify(modelMapper, never()).map(any(), any());
    }
}