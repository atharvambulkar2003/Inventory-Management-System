package com.ims.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ims.dto.AddBatchToProductDto;
import com.ims.dto.ProductDto;
import com.ims.dto.SaleDto;
import com.ims.service.ProductService;

@RestController
@RequestMapping("/api/product")
@CrossOrigin
public class ProductController {
	
	@Autowired
	private ProductService productService;
	
	@PostMapping("/addproduct")
	public ResponseEntity<?> addProduct(@RequestBody ProductDto productDto,Authentication authentication){
		if (authentication == null || !authentication.isAuthenticated()) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
	    }
		try {
			String username = authentication.getName();
			String message = productService.addProduct(productDto,username);
			return new ResponseEntity<>(message,HttpStatus.CREATED);
		}catch(Exception exception) {
			return new ResponseEntity<>(exception.getMessage(),HttpStatus.BAD_REQUEST);
		}
	}
	
	@GetMapping("/getcategory")
	public ResponseEntity<?> getCategory(Authentication authentication){
		if (authentication == null || !authentication.isAuthenticated()) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
	    }
		try {
			String username = authentication.getName();
			List<String> category = productService.getCategory(username);
			return new ResponseEntity<>(category,HttpStatus.CREATED);
		}catch(Exception exception) {
			return new ResponseEntity<>(exception.getMessage(),HttpStatus.BAD_REQUEST);
		}
	}
	
	@PostMapping("/getproductname/{categoryname}")
	public ResponseEntity<?> getProductName(@PathVariable("categoryname") String categoryName,Authentication authentication){
		if (authentication == null || !authentication.isAuthenticated()) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
	    }
		try {
			String username = authentication.getName();
			List<String> products = productService.getProducts(username,categoryName);
			return new ResponseEntity<>(products,HttpStatus.CREATED);
		}catch(Exception exception) {
			return new ResponseEntity<>(exception.getMessage(),HttpStatus.BAD_REQUEST);
		}
	}
	
	@PostMapping("/addbatch")
	public ResponseEntity<?> addBatchToProduct(@RequestBody AddBatchToProductDto addBatchToProductDto,Authentication authentication){
		if (authentication == null || !authentication.isAuthenticated()) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
	    }
		try {
			String username = authentication.getName();
			String message = productService.addBatchToProduct(username,addBatchToProductDto);
			return new ResponseEntity<>(message,HttpStatus.CREATED);
		}catch(Exception exception) {
			return new ResponseEntity<>(exception.getMessage(),HttpStatus.BAD_REQUEST);
		}
	}
	
	@PostMapping("/sale")
	public ResponseEntity<?> sellProduct(@RequestBody SaleDto saleDto,Authentication authentication){
		if (authentication == null || !authentication.isAuthenticated()) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
	    }
		try {
			String username = authentication.getName();
			String message = productService.sellProduct(username,saleDto);
			return new ResponseEntity<>(message,HttpStatus.CREATED);
		}catch(Exception exception) {
			return new ResponseEntity<>(exception.getMessage(),HttpStatus.BAD_REQUEST);
		}
	}
	
}
