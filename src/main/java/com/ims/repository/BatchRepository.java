package com.ims.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ims.entity.BatchEntity;

@Repository
public interface BatchRepository extends JpaRepository<BatchEntity, Long> {

}
