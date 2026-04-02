package com.ims.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ims.dto.TransactionVO;
import com.ims.service.TransactionService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/transaction")
@Slf4j
public class TransactionController {
	
	@Autowired
	private TransactionService transactionService;
	
	@GetMapping
	@PreAuthorize("hasAnyRole('OWNER','STAFF')")
	public ResponseEntity<?> getTransactionHistory(Authentication authentication) {
		log.info("In getTransactionHistory "+authentication.getName());
        String username = authentication.getName();
        List<TransactionVO> transactions = transactionService.getTransactionHistory(username);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
	}
}
