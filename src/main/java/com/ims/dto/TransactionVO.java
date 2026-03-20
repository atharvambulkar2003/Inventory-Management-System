package com.ims.dto;

import java.time.LocalDateTime;

import com.ims.entity.TransactionType;

import lombok.Data;

@Data
public class TransactionVO {
    private Long id;
    private TransactionType type; 
    private Integer quantity;
    private LocalDateTime transactionDate;
    private String partyName; 
    private Double totalAmount;

    private String productName;
    private String productCode;
    private String category;
}