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

import com.ims.dto.TransactionVO;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.TransactionEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.TransactionRepository;
import com.ims.repository.UserRepository;
import com.ims.service.TransactionService;

@ExtendWith(MockitoExtension.class)
class TransactionServiceGetTransactionHistoryTest {

    @Mock private UserRepository userRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private TransactionService transactionService;

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

    private TransactionEntity buildTransaction(Long id, String productCode, String productName,
                                                String category, String defaultUnits) {
        ProductEntity product = new ProductEntity();
        product.setProductCode(productCode);
        product.setProductName(productName);
        product.setCategory(category);
        product.setDefaultUnits(defaultUnits);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setId(id);
        transaction.setProduct(product);
        return transaction;
    }

    @Test
    void getTransactionHistory_success_returnsMappedTransactions() {
        TransactionEntity t1 = buildTransaction(1L, "P001", "PRODUCT ONE", "Electronics", "pcs");
        TransactionEntity t2 = buildTransaction(2L, "P002", "PRODUCT TWO", "Clothing",    "kg");

        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(transactionRepository.findAllByStore(store)).thenReturn(List.of(t1, t2));

        TransactionVO vo1 = new TransactionVO();
        TransactionVO vo2 = new TransactionVO();
        when(modelMapper.map(t1, TransactionVO.class)).thenReturn(vo1);
        when(modelMapper.map(t2, TransactionVO.class)).thenReturn(vo2);

        List<TransactionVO> result = transactionService.getTransactionHistory("owner1");

        assertThat(result).hasSize(2);
        assertThat(vo1.getProductCode()).isEqualTo("P001");
        assertThat(vo1.getProductName()).isEqualTo("PRODUCT ONE");
        assertThat(vo1.getCategory()).isEqualTo("Electronics");
        assertThat(vo1.getDefaultUnits()).isEqualTo("pcs");
        assertThat(vo2.getProductCode()).isEqualTo("P002");
        assertThat(vo2.getProductName()).isEqualTo("PRODUCT TWO");
        verify(modelMapper, times(2)).map(any(), eq(TransactionVO.class));
    }

    @Test
    void getTransactionHistory_noTransactions_returnsEmptyList() {
        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(transactionRepository.findAllByStore(store)).thenReturn(new ArrayList<>());

        List<TransactionVO> result = transactionService.getTransactionHistory("owner1");

        assertThat(result).isEmpty();
        verifyNoInteractions(modelMapper);
    }

    @Test
    void getTransactionHistory_productFieldsSetFromEntity() {
        TransactionEntity t1 = buildTransaction(1L, "P001", "PRODUCT ONE", "Electronics", "pcs");

        when(userRepository.findByUsername("owner1")).thenReturn(user);
        when(transactionRepository.findAllByStore(store)).thenReturn(List.of(t1));

        TransactionVO vo1 = new TransactionVO();
        when(modelMapper.map(t1, TransactionVO.class)).thenReturn(vo1);

        transactionService.getTransactionHistory("owner1");

        assertThat(vo1.getProductCode()).isEqualTo("P001");
        assertThat(vo1.getProductName()).isEqualTo("PRODUCT ONE");
        assertThat(vo1.getCategory()).isEqualTo("Electronics");
        assertThat(vo1.getDefaultUnits()).isEqualTo("pcs");
    }

    @Test
    void getTransactionHistory_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        assertThrows(UserNotFoundException.class,
                () -> transactionService.getTransactionHistory("ghost"));

        verifyNoInteractions(transactionRepository, modelMapper);
    }

    @Test
    void getTransactionHistory_storeNotFound_throwsStoreNotFoundException() {
        user.setStore(null);
        when(userRepository.findByUsername("owner1")).thenReturn(user);

        assertThrows(StoreNotFoundException.class,
                () -> transactionService.getTransactionHistory("owner1"));

        verifyNoInteractions(transactionRepository, modelMapper);
    }
}