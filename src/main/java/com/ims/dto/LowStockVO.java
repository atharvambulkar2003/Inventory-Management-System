package com.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LowStockVO {
	private Long id;
	private String productCode; 
    private String productName;
    private String category;
    private Double minStockLevel;
    private Double totalQuantity;
    private String defaultUnits;
}
