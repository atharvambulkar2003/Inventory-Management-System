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
    private ProductVO productVO;

    @BeforeEach
    void setUp() {
        BatchEntity batchEntity = new BatchEntity();
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

        assertNotNull(result);
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
    void getAllProducts_ReturnsCorrectProductDetails() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(modelMapper.map(activeProduct, ProductVO.class)).thenReturn(productVO);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        ProductVO returned = result.get(0);
        assertEquals("APPLE", returned.getProductName());
        assertEquals("Fruits", returned.getCategory());
        assertEquals(10.0, returned.getTotalQuantity());
        assertEquals(500.0, returned.getTotalPurchasePrice());
    }

    @Test
    void getAllProducts_MultipleActiveProducts_AllReturned() {
        ProductEntity activeProduct2 = new ProductEntity();
        activeProduct2.setProductName("BANANA");
        activeProduct2.setCategory("Fruits");
        activeProduct2.setTotalQuantity(5.0);
        activeProduct2.setTotalPurchasePrice(200.0);
        activeProduct2.setActive(true);
        activeProduct2.setBatches(new ArrayList<>());

        ProductVO productVO2 = new ProductVO();
        productVO2.setProductName("BANANA");

        store.setProducts(new ArrayList<>(List.of(activeProduct, activeProduct2)));

        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(modelMapper.map(activeProduct, ProductVO.class)).thenReturn(productVO);
        when(modelMapper.map(activeProduct2, ProductVO.class)).thenReturn(productVO2);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> "APPLE".equals(p.getProductName())));
        assertTrue(result.stream().anyMatch(p -> "BANANA".equals(p.getProductName())));
    }

    @Test
    void getAllProducts_MixOfActiveAndInactive_ReturnsOnlyActive() {
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(modelMapper.map(activeProduct, ProductVO.class)).thenReturn(productVO);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertEquals(1, result.size());
        assertEquals("APPLE", result.get(0).getProductName());
        verify(modelMapper, never()).map(inactiveProduct, ProductVO.class);
    }

    @Test
    void getAllProducts_EmptyProductList_ReturnsEmptyList() {
        store.setProducts(new ArrayList<>());
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void getAllProducts_AllProductsInactive_ReturnsEmptyList() {
        store.setProducts(new ArrayList<>(List.of(inactiveProduct)));
        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void getAllProducts_ModelMapperCalledForEachActiveProduct() {
        ProductEntity activeProduct2 = new ProductEntity();
        activeProduct2.setProductName("BANANA");
        activeProduct2.setActive(true);
        activeProduct2.setBatches(new ArrayList<>());

        ProductVO productVO2 = new ProductVO();
        productVO2.setProductName("BANANA");

        store.setProducts(new ArrayList<>(List.of(activeProduct, activeProduct2, inactiveProduct)));

        when(userRepository.findByUsername("owneruser")).thenReturn(userEntity);
        when(modelMapper.map(activeProduct, ProductVO.class)).thenReturn(productVO);
        when(modelMapper.map(activeProduct2, ProductVO.class)).thenReturn(productVO2);

        List<ProductVO> result = productService.getAllProducts("owneruser");

        assertEquals(2, result.size());
        verify(modelMapper).map(activeProduct, ProductVO.class);
        verify(modelMapper).map(activeProduct2, ProductVO.class);
        verify(modelMapper, never()).map(inactiveProduct, ProductVO.class);
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