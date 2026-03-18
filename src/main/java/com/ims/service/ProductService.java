package com.ims.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ims.dto.AddBatchToProductDto;
import com.ims.dto.ProductDto;
import com.ims.dto.SaleDto;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.TransactionEntity;
import com.ims.entity.TransactionType;
import com.ims.entity.UserEntity;
import com.ims.repository.BatchRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.TransactionRepository;
import com.ims.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ProductService {
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private BatchRepository batchRepository;
	
	@Autowired
	private TransactionRepository transactionRepository;
	
	@Transactional
	public String addProduct(ProductDto productDto, String username) throws Exception {
		UserEntity userEntity = userRepository.findByUsername(username);
		
		if (userEntity == null) {
            throw new RuntimeException("User not found");
        }
		
		if (productRepository.existsByProductCodeAndStore(productDto.getProductCode(), userEntity.getStore())) {
            throw new Exception("A product with this code already exists in your store.");
        }
		
		if (productRepository.existsByProductNameAndStore(productDto.getProductName().toUpperCase(), userEntity.getStore())) {
	        throw new Exception("A product named '" + productDto.getProductName() + "' is already registered. Please use a unique name or add a variant (e.g., 'Blue', 'XL').");
	    }
		
		ProductEntity productEntity = modelMapper.map(productDto, ProductEntity.class);
		
		productEntity.setStore(userEntity.getStore());
		productEntity.setProductName(productEntity.getProductName().toUpperCase());
		productEntity.setTotalQuantity(0);
		productEntity.setTotalPurchasePrice(0.0);
		
		userEntity.getStore().getProducts().add(productEntity);
		
		productRepository.save(productEntity);
		
		try {
            String subject = "New Product added to your Store";
            String body = "Hello " + userEntity.getFullName() + ",\n\n" +
                    "New product is being added on "+new Date()+".\n\n" +
                    "Product Name : "+productEntity.getProductName()+".\n" +
                    "Product Code : "+productEntity.getProductCode()+".\n" +
                    "Product Category : "+productEntity.getCategory()+".\n" +
                    "Product's Minimum Stock level for alert : "+productEntity.getMinStockLevel()+".\n\n\n\n" +
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(userEntity.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
		
		return "Product Saved successfully";
	}

	public List<String> getCategory(String username) {
		UserEntity userEntity = userRepository.findByUsername(username);
		if (userEntity == null) {
            throw new RuntimeException("User not found");
        }
		StoreEntity store = userEntity.getStore();
		if (store == null) {
	        throw new RuntimeException("Store not found for this user");
	    }
		List<String> categories = productRepository.findDistinctCategoriesByStore(store);
	    return categories;
	}

	public List<String> getProducts(String username, String categoryName) {
		UserEntity userEntity = userRepository.findByUsername(username);
		if (userEntity == null) {
            throw new RuntimeException("User not found");
        }
		StoreEntity store = userEntity.getStore();
		if (store == null) {
	        throw new RuntimeException("Store not found for this user");
	    }
		return productRepository.findProductsByStoreAndCategory(userEntity.getStore(), categoryName);
	}

	@Transactional
    public String addBatchToProduct(String username, AddBatchToProductDto dto) throws Exception {
        UserEntity user = userRepository.findByUsername(username);
        if (user == null || user.getStore() == null) {
            throw new RuntimeException("Store not found for this user");
        }

        ProductEntity product = productRepository.findByProductNameAndStore(dto.getProductName().toUpperCase(), user.getStore());
        
        if (product == null) {
            throw new Exception("Product '" + dto.getProductName() + "' not found. Please add the product first.");
        }

        BatchEntity batch = modelMapper.map(dto, BatchEntity.class);
        batch.setProduct(product);
        batchRepository.save(batch);

        product.setTotalQuantity(product.getTotalQuantity()+dto.getCurrentQuantity());
        product.setTotalPurchasePrice(product.getTotalPurchasePrice()+dto.getPurchasePrice());

        productRepository.save(product);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setProduct(product);
        transaction.setType(TransactionType.PURCHASE);
        transaction.setQuantity(dto.getCurrentQuantity());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setPartyName(dto.getPartyName());
        transaction.setTotalAmount(dto.getPurchasePrice()); 
        
        transactionRepository.save(transaction);
        
        try {
            String subject = "New Batch added to your Store";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                    "New Batch is being added on "+new Date()+" to category "+product.getCategory()+".\n\n" +
                    "Product Name : "+product.getProductName()+".\n\n" +
                    "Batch Number : "+batch.getBatchNumber()+".\n\n" +
                    "Quantity Purchase : "+batch.getCurrentQuantity()+".\n\n" +
                    "Total Quantity : "+product.getTotalQuantity()+".\n\n" +
                    "Price of batch : "+batch.getPurchasePrice()+".\n\n" +
                    "Total Price : "+product.getTotalPurchasePrice()+".\n\n\n\n\n" +
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return "Batch added Successfully";
    }

	@Transactional
	public String sellProduct(String username, SaleDto saleDto) {
		UserEntity user = userRepository.findByUsername(username);
        if (user == null || user.getStore() == null) {
            throw new RuntimeException("Store not found for this user");
        }
        ProductEntity productEntity = productRepository.findByProductNameAndCategoryAndStore(saleDto.getProductName(),saleDto.getCategory(), user.getStore());
		
        if(productEntity.getTotalQuantity()<saleDto.getQuantity()) {
        	throw new RuntimeException("Quantity is less, total Quantity of "+productEntity.getProductName()+" is "+productEntity.getTotalQuantity());
        }
        
        
        return null;
	}
	
	

}
