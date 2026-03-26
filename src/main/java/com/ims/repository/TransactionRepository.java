package com.ims.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ims.entity.StoreEntity;
import com.ims.entity.TransactionEntity;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>{
	@Query("SELECT t FROM TransactionEntity t WHERE t.product.store = :store ORDER BY t.transactionDate DESC")
    List<TransactionEntity> findAllByStore(@Param("store") StoreEntity store);
	
}
