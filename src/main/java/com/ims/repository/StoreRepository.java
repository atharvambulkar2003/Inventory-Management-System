package com.ims.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ims.entity.StoreEntity;

@Repository
public interface StoreRepository extends JpaRepository<StoreEntity, Long>{
	boolean existsByGstNumber(String gstNumbser);
}
