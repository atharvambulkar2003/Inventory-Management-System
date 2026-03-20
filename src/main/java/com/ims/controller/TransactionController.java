package com.ims.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ims.dto.TransactionVO;
import com.ims.service.TransactionService;

@RestController
@RequestMapping("/api/transaction")
public class TransactionController {
	
	@Autowired
	private TransactionService transactionService;
	
	@GetMapping("/alltransactions")
	public ResponseEntity<?> getTransactionHistory(Authentication authentication) {
	    if (authentication == null || !authentication.isAuthenticated()) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
	    }
	    try {
	        String username = authentication.getName();
	        List<TransactionVO> transactions = transactionService.getTransactionHistory(username);
	        return new ResponseEntity<>(transactions, HttpStatus.OK);
	    } catch (Exception exception) {
	        return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
	    }
	}
}
