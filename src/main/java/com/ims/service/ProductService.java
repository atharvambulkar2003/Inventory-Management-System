package com.ims.service;

import java.util.List;

import com.ims.dto.AddBatchToProductDto;
import com.ims.dto.BatchExpiryVO;
import com.ims.dto.BatchUpdateDto;
import com.ims.dto.LowStockVO;
import com.ims.dto.ProductDto;
import com.ims.dto.ProductEditDto;
import com.ims.dto.ProductVO;
import com.ims.dto.SaleDto;

import jakarta.transaction.Transactional;

public interface ProductService {

	String addProduct(ProductDto productDto, String username);

	List<String> getCategory(String username);

	List<String> getProducts(String username, String categoryName);

	String addBatchToProduct(String username, AddBatchToProductDto dto);

	String sellProduct(String username, SaleDto saleDto);

	List<ProductVO> getAllProducts(String username);

	String updateProduct(ProductEditDto productEditDto, String username);

	String deleteProduct(String productCode, String username);

	String updateBatch(String username, BatchUpdateDto dto);

	String deleteBatch(String username, Long id);

	List<LowStockVO> getLowStockItems(String username);

	List<BatchExpiryVO> getExpiryItems(String username);

	String getProductUnit(String username, String productName);

	boolean checkProductCodeExists(String username, String productCode);

	String generateFiveDigitCode();

	boolean checkBatchNumberExists(String username, String batchNumber);

}