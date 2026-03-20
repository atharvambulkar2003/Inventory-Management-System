package com.ims.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class BatchExpiryVO {
    private Long id;
    private String batchNumber;
    private Integer currentQuantity;
    private LocalDate expiryDate;
    private String productName;
    private String productCode;
}