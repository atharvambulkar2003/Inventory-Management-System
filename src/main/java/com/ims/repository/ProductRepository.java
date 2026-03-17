package com.ims.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long>{
	boolean existsByProductCodeAndStore(String productCode,StoreEntity storeEntity);
}
