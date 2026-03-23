package com.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleDto {
	private String category;
	private String productName;
	private Double quantity;
	private Double interestRate;
	private String customerName;
}
