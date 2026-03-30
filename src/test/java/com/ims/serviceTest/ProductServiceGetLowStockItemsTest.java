package com.ims.serviceTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.ims.dto.LowStockVO;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.UserRepository;
import com.ims.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductServiceGetLowStockItemsTest {

    @Mock private UserRepository userRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private ProductService productService;

    private UserEntity user;
    private StoreEntity store;

    @BeforeEach
    void setUp() {
        store = new StoreEntity();
        store.setId(1L);

        user = new UserEntity();
        user.setUsername("owner1");
        user.setStore(store);
    }

    private ProductEntity buildProduct(Long id, boolean active, double totalQty, double minStock) {
        ProductEntity p = new ProductEntity();
        p.setId(id);
        p.setActive(active);
        p.setTotalQuantity(totalQty);
        p.setMinStockLevel(minStock);
        return p;
    }

    @Test
    void getLowStockItems_success_returnsOnlyLowStockActiveProducts() {
        ProductEntity lowStock    = buildProduct(1L, true,  5.0,  10.0);
        ProductEntity normalStock = buildProduct(2L, true,  20.0, 10.0);
        ProductEntity inactive    = buildProduct(3L, false, 2.0,  10.0);
        store.setProducts(List.of(lowStock, normalStock, inactive));

        LowStockVO vo = new LowStockVO();
        vo.setId(1L);
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(modelMapper.map(lowStock, LowStockVO.class)).thenReturn(vo);

        List<LowStockVO> result = productService.getLowStockItems("owner1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(modelMapper, times(1)).map(any(), eq(LowStockVO.class));
    }

    @Test
    void getLowStockItems_noLowStockProducts_returnsEmptyList() {
        ProductEntity normalStock = buildProduct(1L, true, 20.0, 10.0);
        store.setProducts(List.of(normalStock));

        when(userRepository.findByUsername("owner1")).thenReturn(user);

        List<LowStockVO> result = productService.getLowStockItems("owner1");

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }

    @Test
    void getLowStockItems_emptyProductList_returnsEmptyList() {
        store.setProducts(new ArrayList<>());

        when(userRepository.findByUsername("owner1")).thenReturn(user);

        List<LowStockVO> result = productService.getLowStockItems("owner1");

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }

    @Test
    void getLowStockItems_allProductsLowStock_returnsAll() {
        ProductEntity p1 = buildProduct(1L, true, 2.0, 10.0);
        ProductEntity p2 = buildProduct(2L, true, 3.0, 15.0);
        store.setProducts(List.of(p1, p2));

        LowStockVO vo1 = new LowStockVO(); vo1.setId(1L);
        LowStockVO vo2 = new LowStockVO(); vo2.setId(2L);
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(modelMapper.map(p1, LowStockVO.class)).thenReturn(vo1);
        when(modelMapper.map(p2, LowStockVO.class)).thenReturn(vo2);

        List<LowStockVO> result = productService.getLowStockItems("owner1");

        assertThat(result).hasSize(2);
        verify(modelMapper, times(2)).map(any(), eq(LowStockVO.class));
    }

    @Test
    void getLowStockItems_quantityExactlyAtMinStock_notIncluded() {
        ProductEntity atMinStock = buildProduct(1L, true, 10.0, 10.0);
        store.setProducts(List.of(atMinStock));

        when(userRepository.findByUsername("owner1")).thenReturn(user);

        List<LowStockVO> result = productService.getLowStockItems("owner1");

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }

    @Test
    void getLowStockItems_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> productService.getLowStockItems("ghost"));

        verifyNoInteractions(modelMapper);
    }

    @Test
    void getLowStockItems_storeNotFound_throwsStoreNotFoundException() {
        user.setStore(null);
        when(userRepository.findByUsername("owner1")).thenReturn(user);

        assertThrows(StoreNotFoundException.class,
                () -> productService.getLowStockItems("owner1"));

        verifyNoInteractions(modelMapper);
    }
}