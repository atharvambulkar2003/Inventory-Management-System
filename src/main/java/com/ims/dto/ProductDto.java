package com.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {
	private String productCode; 
    private String productName;
    private String category;
    private Integer minStockLevel;
    private Double interestRate;
}
