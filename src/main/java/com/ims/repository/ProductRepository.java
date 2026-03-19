package com.ims.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long>{
	boolean existsByProductCodeAndStore(String productCode,StoreEntity storeEntity);
	boolean existsByProductNameAndStore(String productName, StoreEntity store);
	@Query("SELECT DISTINCT p.category FROM ProductEntity p WHERE p.store = :store")
    List<String> findDistinctCategoriesByStore(@Param("store") StoreEntity store);	
	@Query("SELECT DISTINCT p.productName FROM ProductEntity p WHERE p.store = :store AND p.category = :category")
    List<String> findProductsByStoreAndCategory(@Param("store") StoreEntity store, @Param("category") String category);
	ProductEntity findByProductNameAndStore(String productName, StoreEntity store);
	ProductEntity findByProductNameAndCategoryAndStore(String productName, String category, StoreEntity store);
	ProductEntity findByProductCodeAndStore(String productCode, StoreEntity store);
}
