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
    private Double minStockLevel;
    private Double totalQuantity;
    private Double totalPurchasePrice;
    private Double perItemPrice;
    private String defaultUnits;
    List<BatchVO> batches;
}
