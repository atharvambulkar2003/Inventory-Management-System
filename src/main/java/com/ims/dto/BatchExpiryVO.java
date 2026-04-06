package com.ims.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class BatchExpiryVO {
    private Long id;
    private String batchNumber;
    private Double currentQuantity;
    private LocalDate expiryDate;
    private String location;
    private String productName;
    private String productCode;
    private String defaultUnits;
}