package com.ims.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "batches")
@Data
@ToString(exclude = "product")
public class BatchEntity {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
	
	@Version
    private Long version;

    private String batchNumber;
    private Double currentQuantity;
    private LocalDate expiryDate; 
    private Double purchasePrice;
}
