package com.ims.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsByProductCodeAndStoreAndActiveTrue(String productCode, StoreEntity store);
    
    boolean existsByProductNameAndStoreAndActiveTrue(String productName, StoreEntity store);
   
    @Query("SELECT DISTINCT p.category FROM ProductEntity p WHERE p.store = :store AND p.active = true")
    List<String> findDistinctCategoriesByStoreAndActiveTrue(@Param("store") StoreEntity store);

    @Query("SELECT p.productName FROM ProductEntity p WHERE p.store = :store AND p.category = :category AND p.active = true")
    List<String> findProductsByStoreAndCategoryAndActiveTrue(@Param("store") StoreEntity store, @Param("category") String category);

    ProductEntity findByProductNameAndStoreAndActiveTrue(String productName, StoreEntity store);

    @Lock(LockModeType.PESSIMISTIC_WRITE) 
    @Query("SELECT p FROM ProductEntity p WHERE p.productName = :productName AND p.category = :category AND p.store = :store AND p.active = true")
    ProductEntity findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
        @Param("productName") String productName, 
        @Param("category") String category, 
        @Param("store") StoreEntity store
    );
    
    ProductEntity findByProductCodeAndStoreAndActiveTrue(String productCode, StoreEntity store);

}