package com.ims.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductVO {
	private String productCode; 
    private String productName;
    private String category;
    private Integer minStockLevel;
    private Integer totalQuantity;
    private Double totalPurchasePrice;
    private Double perItemPrice;
    List<BatchVO> batches;
}
