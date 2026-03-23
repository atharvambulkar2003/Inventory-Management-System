package com.ims.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddBatchToProductDto {
	private String category;
	private String productName;
	@JsonProperty("batchNo")
	private String batchNumber;
	@JsonProperty("currQuantity")
	private Double currentQuantity;
	private LocalDate expiryDate; 
    private Double purchasePrice;
    private String partyName; 
}
