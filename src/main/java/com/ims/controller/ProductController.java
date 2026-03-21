package com.ims.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ims.dto.AddBatchToProductDto;
import com.ims.dto.BatchExpiryVO;
import com.ims.dto.BatchUpdateDto;
import com.ims.dto.LowStockVO;
import com.ims.dto.ProductDto;
import com.ims.dto.ProductEditDto;
import com.ims.dto.ProductVO;
import com.ims.dto.SaleDto;
import com.ims.service.ProductService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/product")
@CrossOrigin
@Slf4j
public class ProductController {
	
	@Autowired
	private ProductService productService;
	
	@PostMapping("/addproduct")
	public ResponseEntity<?> addProduct(@RequestBody ProductDto productDto,Authentication authentication){
		log.info("In addProduct "+productDto+" "+authentication.getName());
		String username = authentication.getName();
		String message = productService.addProduct(productDto,username);
		return new ResponseEntity<>(message,HttpStatus.CREATED);
	}
	
	@GetMapping("/getcategory")
	public ResponseEntity<?> getCategory(Authentication authentication){
		log.info("In getCategory"+" "+authentication.getName());
		String username = authentication.getName();
		List<String> category = productService.getCategory(username);
		return new ResponseEntity<>(category,HttpStatus.CREATED);
	}
	
	@PostMapping("/getproductname/{categoryname}")
	public ResponseEntity<?> getProductName(@PathVariable("categoryname") String categoryName,Authentication authentication){
		log.info("In getProductName "+categoryName+" "+authentication.getName());
		String username = authentication.getName();
		List<String> products = productService.getProducts(username,categoryName);
		return new ResponseEntity<>(products,HttpStatus.CREATED);
	}
	
	@PostMapping("/addbatch")
	public ResponseEntity<?> addBatchToProduct(@RequestBody AddBatchToProductDto addBatchToProductDto,Authentication authentication){
		log.info("In addBatchToProduct "+addBatchToProductDto+" "+authentication.getName());
		String username = authentication.getName();
		String message = productService.addBatchToProduct(username,addBatchToProductDto);
		return new ResponseEntity<>(message,HttpStatus.CREATED);
	}
	
	@PostMapping("/sale")
	public ResponseEntity<?> sellProduct(@RequestBody SaleDto saleDto,Authentication authentication){
		log.info("In sellProduct "+saleDto+" "+authentication.getName());
		String username = authentication.getName();
		String message = productService.sellProduct(username,saleDto);
		return new ResponseEntity<>(message,HttpStatus.CREATED);
	}
	
	@GetMapping("/allproducts")
	public ResponseEntity<?> getAllProducts(Authentication authentication){
		log.info("In getAllProducts"+authentication.getName());
		String username = authentication.getName();
		List<ProductVO> productVO = productService.getAllProducts(username);
		return new ResponseEntity<>(productVO,HttpStatus.CREATED);
	}
	
	@PutMapping("/updateproduct")
	public ResponseEntity<?> updateProduct(@RequestBody ProductEditDto productEditDto,Authentication authentication){
		log.info("In updateProduct "+productEditDto+" "+authentication.getName());
		String username = authentication.getName();
		String message = productService.updateProduct(productEditDto,username);
		return new ResponseEntity<>(message,HttpStatus.CREATED);
	}
	
	@DeleteMapping("/deleteproduct/{productCode}")
	public ResponseEntity<?> deleteProduct(@PathVariable String productCode,Authentication authentication){
		log.info("In updateProduct "+productCode+" "+authentication.getName());
		String username = authentication.getName();
		String message = productService.deleteProduct(productCode,username);
		return new ResponseEntity<>(message,HttpStatus.CREATED);
	}
	
	@PutMapping("/updatebatch")
	public ResponseEntity<?> updateBatch(@RequestBody BatchUpdateDto batchUpdateDto, Authentication authentication) {
		log.info("In updateProduct "+batchUpdateDto+" "+authentication.getName());
		String username = authentication.getName();
        String message = productService.updateBatch(username, batchUpdateDto);
        return new ResponseEntity<>(message, HttpStatus.OK);
	}
	
	@DeleteMapping("/deletebatch/{id}")
	public ResponseEntity<?> deleteBatch(@PathVariable Long id, Authentication authentication) {
		log.info("In deleteBatch "+id+" "+authentication.getName());
		String username = authentication.getName();
        String message = productService.deleteBatch(username, id);
        return new ResponseEntity<>(message, HttpStatus.OK);
	}
	
	@GetMapping("/lowstockitems")
	public ResponseEntity<?> getLowStockItems(Authentication authentication) {
		log.info("In getLowStockItems"+authentication.getName());
		String username = authentication.getName();
        List<LowStockVO> lowStockVO = productService.getLowStockItems(username);
        return new ResponseEntity<>(lowStockVO, HttpStatus.OK);
	}
	
	@GetMapping("/expiryitems")
	public ResponseEntity<?> getExpiryItems(Authentication authentication) {
		log.info("In getExpiryItems"+authentication.getName());
		String username = authentication.getName();
        List<BatchExpiryVO> expiryItems = productService.getExpiryItems(username);
        return new ResponseEntity<>(expiryItems, HttpStatus.OK);
	}
	
}
