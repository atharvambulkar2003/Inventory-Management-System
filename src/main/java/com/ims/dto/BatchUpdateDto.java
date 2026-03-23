package com.ims.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchUpdateDto {
	private Long id;
	private String batchNumber;
    private Double currentQuantity;
    private LocalDate expiryDate; 
    private Double purchasePrice;
}
