package com.ims.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchVO {
	private Long id;
	private String batchNumber;
    private Double currentQuantity;
    private LocalDate expiryDate; 
    private Double purchasePrice;
}
