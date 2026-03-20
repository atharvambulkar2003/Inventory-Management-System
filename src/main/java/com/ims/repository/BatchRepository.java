package com.ims.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ims.entity.BatchEntity;

@Repository
public interface BatchRepository extends JpaRepository<BatchEntity, Long> {
	@Query("SELECT b FROM BatchEntity b " +"WHERE b.product.store.owner.username = :username " +"AND b.expiryDate BETWEEN :startDate AND :endDate " +"AND b.currentQuantity > 0 " +"ORDER BY b.expiryDate ASC")
    List<BatchEntity> findExpiringBatches(@Param("username") String username, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
