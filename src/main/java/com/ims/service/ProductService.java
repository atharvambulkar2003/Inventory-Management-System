package com.ims.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ims.dto.AddBatchToProductDto;
import com.ims.dto.BatchExpiryVO;
import com.ims.dto.BatchUpdateDto;
import com.ims.dto.BatchVO;
import com.ims.dto.LowStockVO;
import com.ims.dto.ProductDto;
import com.ims.dto.ProductEditDto;
import com.ims.dto.ProductVO;
import com.ims.dto.SaleDto;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.TransactionEntity;
import com.ims.entity.TransactionType;
import com.ims.entity.UserEntity;
import com.ims.exception.BatchDoesNotBelongToUserException;
import com.ims.exception.BatchNotFoundException;
import com.ims.exception.LessQuantityException;
import com.ims.exception.ProductCodeAlreadyExistsInStoreException;
import com.ims.exception.ProductNameAlreadyExistsInStore;
import com.ims.exception.ProductNotFoundException;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.BatchRepository;
import com.ims.repository.ProductRepository;
import com.ims.repository.TransactionRepository;
import com.ims.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
	public String addProduct(ProductDto productDto, String username) {
		log.info("In addProduct service "+productDto+" "+username);
		
		UserEntity userEntity = userRepository.findByUsername(username);
		
		if (userEntity == null) {
			log.error("User not found "+username);
            throw new UserNotFoundException("User Not Found");
        }
		
		if (productRepository.existsByProductCodeAndStore(productDto.getProductCode(), userEntity.getStore())) {
			log.error("A product with this code already exists in your store, "+productDto);
            throw new ProductCodeAlreadyExistsInStoreException("A product with this code already exists in your store.");
        }
		
		if (productRepository.existsByProductNameAndStore(productDto.getProductName().toUpperCase(), userEntity.getStore())) {
			log.error("A product named '" + productDto.getProductName() + "' is already registered. Please use a unique name or add a variant (e.g., 'Blue', 'XL').");
	        throw new ProductNameAlreadyExistsInStore("A product named '" + productDto.getProductName() + "' is already registered. Please use a unique name or add a variant (e.g., 'Blue', 'XL').");
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
        	log.error("Failed to send email: " + e.getMessage());
        }
		
		return "Product Saved successfully";
	}

	public List<String> getCategory(String username) {
		log.info("In getCategory service "+username);

		UserEntity userEntity = userRepository.findByUsername(username);
		if (userEntity == null) {
        	log.error("User not found "+username);
            throw new UserNotFoundException("User not found");
        }
		StoreEntity store = userEntity.getStore();
		if (store == null) {
        	log.error("Store not found "+username);
	        throw new StoreNotFoundException("Store not found for this user");
	    }
		List<String> categories = productRepository.findDistinctCategoriesByStore(store);
	    return categories;
	}

	public List<String> getProducts(String username, String categoryName) {
		UserEntity userEntity = userRepository.findByUsername(username);
		if (userEntity == null) {
        	log.error("User not found "+username);
            throw new UserNotFoundException("User not found");
        }
		StoreEntity store = userEntity.getStore();
		if (store == null) {
        	log.error("Store not found "+username);
	        throw new StoreNotFoundException("Store not found for this user");
	    }
		return productRepository.findProductsByStoreAndCategory(userEntity.getStore(), categoryName);
	}

	@Transactional
    public String addBatchToProduct(String username, AddBatchToProductDto dto) {
		log.info("In addBatchToProduct service "+username+" "+dto);

        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
        	log.error("User not found "+username);
            throw new UserNotFoundException("User not found");
        }
        
        if(user.getStore() == null) {
        	log.error("Store not found "+username);
        	throw new StoreNotFoundException("Store not found");
        }

        ProductEntity product = productRepository.findByProductNameAndStore(dto.getProductName().toUpperCase(), user.getStore());
        
        if (product == null) {
        	log.error("Product '" + dto.getProductName() + "' not found.");
            throw new ProductNotFoundException("Product '" + dto.getProductName() + "' not found. Please add the product first.");
        }

        BatchEntity batch = modelMapper.map(dto, BatchEntity.class);
        batch.setProduct(product);
        batchRepository.save(batch);

        product.setTotalQuantity(product.getTotalQuantity()+dto.getCurrentQuantity());
        product.setTotalPurchasePrice(product.getTotalPurchasePrice()+dto.getPurchasePrice());

        TransactionEntity transaction = new TransactionEntity();
        transaction.setProduct(product);
        transaction.setType(TransactionType.PURCHASE);
        transaction.setQuantity(dto.getCurrentQuantity());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setPartyName(dto.getPartyName());
        transaction.setTotalAmount(dto.getPurchasePrice()); 
        
        transactionRepository.save(transaction);
        
        product.getTransactions().add(transaction);
        
        productRepository.save(product);
        
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
        	log.error("Failed to send welcome email: " + e.getMessage());
        }

        return "Batch added Successfully";
    }

	@Transactional
	public String sellProduct(String username, SaleDto saleDto) {
		log.info("In sellProduct service "+username+" "+saleDto);

		UserEntity user = userRepository.findByUsername(username);
		if (user == null) {
			log.error("User not found "+username);
            throw new UserNotFoundException("User not found");
        }
        
        if(user.getStore() == null) {
        	log.error("Store not found "+username);
        	throw new StoreNotFoundException("Store not found");
        }
        
        ProductEntity productEntity = productRepository.findByProductNameAndCategoryAndStore(saleDto.getProductName().toUpperCase(),saleDto.getCategory(), user.getStore());
		
        if(productEntity.getTotalQuantity()<saleDto.getQuantity()) {
        	log.error("Quantity is less, total Quantity of "+productEntity.getProductName()+" is "+productEntity.getTotalQuantity());
        	throw new LessQuantityException("Quantity is less, total Quantity of "+productEntity.getProductName()+" is "+productEntity.getTotalQuantity());
        }
        
        Double individualProductPrice = productEntity.getTotalPurchasePrice() / productEntity.getTotalQuantity();
        Double totalPriceWithQuantitySendByUser = individualProductPrice * saleDto.getQuantity();
        Double sellingPrice = totalPriceWithQuantitySendByUser + (totalPriceWithQuantitySendByUser * saleDto.getInterestRate() / 100);
        sellingPrice = Math.round(sellingPrice * 100.0) / 100.0;
        
        List<BatchEntity> batches = productEntity.getBatches();
        
        Integer tempQuantity = saleDto.getQuantity();
        
        while(tempQuantity > 0 && !batches.isEmpty()) {
        	BatchEntity batch = batches.get(0);
        	if(batch.getCurrentQuantity() <= tempQuantity) {
        		tempQuantity = tempQuantity - batch.getCurrentQuantity();
        		batches.remove(0);
        		batchRepository.delete(batch);
        	}else {
        		batch.setCurrentQuantity(batch.getCurrentQuantity()-tempQuantity);
        		tempQuantity = 0;
        		batchRepository.save(batch);
        	}
        }
        
        productEntity.setTotalPurchasePrice(productEntity.getTotalPurchasePrice()-totalPriceWithQuantitySendByUser);
        productEntity.setTotalQuantity(productEntity.getTotalQuantity()-saleDto.getQuantity());
        
        TransactionEntity transaction = new TransactionEntity();
        transaction.setProduct(productEntity);
        transaction.setType(TransactionType.SALE);
        transaction.setQuantity(saleDto.getQuantity());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setPartyName(saleDto.getCustomerName());
        transaction.setTotalAmount(sellingPrice); 
        
        transactionRepository.save(transaction);
        
        productEntity.getTransactions().add(transaction);
        productRepository.save(productEntity);
        
        try {
            String subject = "Sale of "+productEntity.getProductName()+" is sussessful";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                    "Product Name : "+productEntity.getProductName()+".\n\n" +
                    "Total Quantity : "+saleDto.getQuantity()+".\n\n" +
                    "Interest Rate : "+saleDto.getInterestRate()+".\n\n" +
                    "Total cost price : "+totalPriceWithQuantitySendByUser+".\n\n" +
                    "Selling Price : "+sellingPrice+".\n\n" +
                    "Profit : "+(sellingPrice-totalPriceWithQuantitySendByUser)+".\n\n" +
                    "Customer : "+saleDto.getCustomerName()+".\n\n" +
                    "Quantity Remaining of"+productEntity.getProductName()+" product : "+productEntity.getTotalQuantity()+".\n\n\n\n\n" +
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
        	log.error("Failed to send email: " + e.getMessage());
        }
        
        return "Sale is done with selling price "+ sellingPrice+ " on interest rate "+ saleDto.getInterestRate();
	}

	public List<ProductVO> getAllProducts(String username) {
		log.info("In getAllProducts service "+username);

	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        
        if(user.getStore() == null) {
        	throw new StoreNotFoundException("Store not found");
        } 
	    
	    List<ProductEntity> products = user.getStore().getProducts();        
	    List<ProductVO> listProductVO = new ArrayList<>();
	    
	    for (ProductEntity product : products) {
	        ProductVO productVO = modelMapper.map(product, ProductVO.class);
	        
	        List<BatchVO> listBatchVO = new ArrayList<>();
	        for (BatchEntity batch : product.getBatches()) {
	            listBatchVO.add(modelMapper.map(batch, BatchVO.class));
	        }
	        if (product.getTotalQuantity() != null && product.getTotalQuantity() > 0) {
	        	Double rawPerItemPrice = product.getTotalPurchasePrice() / product.getTotalQuantity();
	        	productVO.setPerItemPrice(Math.round(rawPerItemPrice * 100.0) / 100.0);
	        } else {
	            productVO.setPerItemPrice(0.0); 
	        }	        productVO.setBatches(listBatchVO);
	        listProductVO.add(productVO);
	    }
	    
	    return listProductVO;
	}
	
	@Transactional
	public String updateProduct(ProductEditDto productEditDto, String username) {
		log.info("In updateProduct service "+username+" "+productEditDto);

	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) {
	    	log.error("User not found "+ username);
            throw new UserNotFoundException("User not found");
        }
        
        if(user.getStore() == null) {
        	log.error("Store not found "+username);
        	throw new StoreNotFoundException("Store not found");
        } 

	    ProductEntity productEntity = productRepository.findByProductCodeAndStore(productEditDto.getProductCode(), user.getStore());
	    
	    if (productEntity == null) {
	    	log.error("Product not found with code: " + productEditDto.getProductCode());
	        throw new ProductNotFoundException("Product not found with code: " + productEditDto.getProductCode());
	    }

	    ProductEntity duplicateNameProduct = productRepository.findByProductNameAndStore(productEditDto.getProductName().toUpperCase(), user.getStore());
	        
	    if (duplicateNameProduct != null && !duplicateNameProduct.getId().equals(productEntity.getId())) {
	    	log.error("Product name '" + productEditDto.getProductName() + "' is already used by another product.");
	        throw new ProductNameAlreadyExistsInStore("Product name '" + productEditDto.getProductName() + "' is already used by another product.");
	    }

	    productEntity.setCategory(productEditDto.getCategory());
	    productEntity.setMinStockLevel(productEditDto.getMinStockLevel());
	    productEntity.setProductName(productEditDto.getProductName().toUpperCase());
	    
	    productRepository.save(productEntity);
	    
	    try {
            String subject = "Update of product is successfully done";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                    "Old product name : "+productEntity.getProductName()+".\n" +
                    "New product name : "+productEditDto.getProductName()+".\n" +
                    "Old product category  : "+productEntity.getCategory()+".\n" +
                    "New product category : "+productEditDto.getCategory()+".\n" +
                    "Old product minimum stock level : "+productEntity.getMinStockLevel()+".\n" +
                    "New Product minimum stock level : "+productEditDto.getMinStockLevel()+".\n" +
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
        	log.error("Failed to send email: " + e.getMessage());
        }
	    
	    return "Product Updated successfully";
	}

	@Transactional
	public String deleteProduct(String productCode, String username) {
		log.info("In deleteProduct service "+username+" "+productCode);

		UserEntity user = userRepository.findByUsername(username);
		if (user == null) {
			log.error("User not found "+username);
            throw new UserNotFoundException("User not found");
        }
        
        if(user.getStore() == null) {
        	log.error("Store not found "+username);
        	throw new StoreNotFoundException("Store not found");
        }
	    ProductEntity productEntity = productRepository.findByProductCodeAndStore(productCode, user.getStore());
	    
	    if (productEntity == null) {
	    	log.error("Product not found with code: " +productCode);
	        throw new ProductNotFoundException("Product not found with code: " +productCode);
	    }
	    
	    if (productEntity.getTransactions() != null) {
	        for (TransactionEntity transaction : productEntity.getTransactions()) {
	            transaction.setProduct(null); 
	            transactionRepository.save(transaction);
	        }
	    }
	    
	    productRepository.delete(productEntity);
	    
	    try {
            String subject = "Product "+productEntity.getProductName()+" deleted successfully";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                    "Your product "+productEntity.getProductName()+" and its batches is deleted successfully.\n\n\n" +
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
        	log.error("Failed to send email: " + e.getMessage());
        }
	    
		return "Product Deleted successfully";
	} 
	
	@Transactional
	public String updateBatch(String username, BatchUpdateDto dto) {
		log.info("In updateBatch service "+username+" "+dto);

	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) {
	    	log.error("User not found "+username);
            throw new UserNotFoundException("User not found");
        }
        
        if(user.getStore() == null) {
        	log.error("Store not found "+username);
        	throw new StoreNotFoundException("Store not found");
        }

	    BatchEntity batch = batchRepository.findById(dto.getId())
	            .orElseThrow(() -> new BatchNotFoundException("Batch not found with ID: " + dto.getId()));

	    if (!batch.getProduct().getStore().getId().equals(user.getStore().getId())) {
	    	log.error("Access denied to batch "+username);
	        throw new BatchDoesNotBelongToUserException("This batch does not belong to your store.");
	    }

	    ProductEntity product = batch.getProduct();
	    
	    int quantityDifference = dto.getCurrentQuantity() - batch.getCurrentQuantity();
	    product.setTotalQuantity(product.getTotalQuantity() + quantityDifference);
	    
	    double priceDifference = dto.getPurchasePrice() - batch.getPurchasePrice();
	    product.setTotalPurchasePrice(product.getTotalPurchasePrice() + priceDifference);

	    batch.setBatchNumber(dto.getBatchNumber());
	    batch.setCurrentQuantity(dto.getCurrentQuantity());
	    batch.setExpiryDate(dto.getExpiryDate());
	    batch.setPurchasePrice(dto.getPurchasePrice());

	    batchRepository.save(batch);
	    productRepository.save(product);

	    try {
            String subject = "Update of batch is successfully done";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                    "Old batch number : "+batch.getBatchNumber()+".\n" +
                    "Updated batch number : "+dto.getBatchNumber()+".\n" +
                    "Old batch quantity  : "+batch.getCurrentQuantity()+".\n" +
                    "Updated batch quantity : "+dto.getCurrentQuantity()+".\n" +
                    "Old batch purchase price : "+batch.getPurchasePrice()+".\n" +
                    "Updated batch purchase price : "+dto.getPurchasePrice()+".\n" +
                    "Old batch expiry date : " + (batch.getExpiryDate() == null ? "No expiry" : batch.getExpiryDate()) + ".\n" +
                    "Updated batch expiry date : " + (dto.getExpiryDate() == null ? "No expiry" : dto.getExpiryDate()) + ".\n"+
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
        	log.error("Failed to send welcome email: " + e.getMessage());
        }
	    
	    return "Batch " + batch.getBatchNumber() + " updated successfully.";
	}
	
	@Transactional
	public String deleteBatch(String username, Long id) {
		log.info("In deleteBatch service "+username+" id : "+id);

	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) {
	    	log.error("User not found "+username);
	        throw new UserNotFoundException("Store not found");
	    }
	    
	    if(user.getStore() == null) {
	    	log.error("Store not found "+username);
	    	throw new StoreNotFoundException("Store not found");
	    }

	    BatchEntity batch = batchRepository.findById(id)
	            .orElseThrow(() -> new BatchNotFoundException("Batch not found with ID: " + id));

	    ProductEntity product = batch.getProduct();
	    if (!product.getStore().getId().equals(user.getStore().getId())) {
	    	log.error("Access denied to batch "+username);
	        throw new BatchDoesNotBelongToUserException("Access denied to this batch.");
	    }

	    product.setTotalQuantity(product.getTotalQuantity() - batch.getCurrentQuantity());
	    product.setTotalPurchasePrice(product.getTotalPurchasePrice() - batch.getPurchasePrice());

	    product.getBatches().remove(batch);
	    batchRepository.delete(batch);
	    
	    productRepository.save(product);
	    
	    try {
            String subject = "Batch "+batch.getBatchNumber()+" is deleted successfully";
            String body = "Hello " + user.getFullName() + ",\n\n" +
                    "Your batch "+batch.getBatchNumber()+" is deleted successfully.\n\n\n" +
                    "Regards,\n" +
                    "Inventry Management System";
            emailService.sendSimpleEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
        	log.error("Failed to send welcome email: " + e.getMessage());
        }

	    return "Batch " + batch.getBatchNumber() + " deleted.";
	}

	public List<LowStockVO> getLowStockItems(String username) {
		log.info("In getLowStockItems "+username);
		UserEntity user = userRepository.findByUsername(username);
		if (user == null) {
			log.error("User not found "+username);
            throw new UserNotFoundException("User not found");
        }
        
        if(user.getStore() == null) {
        	log.error("Store not found "+username);
        	throw new StoreNotFoundException("Store not found");
        }
        
	    List<ProductEntity> listProductEntities = user.getStore().getProducts(); 
	    List<LowStockVO> listLowStockVO = new ArrayList<>();
	    
	    for(ProductEntity product:listProductEntities) {
	    	if(product.getTotalQuantity()<=product.getMinStockLevel()) {
	    		LowStockVO lowStockVO = modelMapper.map(product, LowStockVO.class);
	    		listLowStockVO.add(lowStockVO);
	    	}
	    }
	    
		return listLowStockVO;
	}

	public List<BatchExpiryVO> getExpiryItems(String username) {
		log.info("In getExpiryItems "+ username);
		UserEntity user = userRepository.findByUsername(username);
		if (user == null) {
			log.error("User not found "+username);
            throw new UserNotFoundException("User not found");
        }
        
        if(user.getStore() == null) {
        	log.error("Store not found "+username);
        	throw new StoreNotFoundException("Store not found");
        }
        
	    LocalDate today = LocalDate.now();
	    LocalDate thirtyDaysFromNow = today.plusDays(30);
	    List<BatchEntity> batches = batchRepository.findExpiringBatches(username, today, thirtyDaysFromNow);
	    List<BatchExpiryVO> expiryItems = new ArrayList<>();
	    for (BatchEntity batch : batches) {
	        BatchExpiryVO vo = modelMapper.map(batch, BatchExpiryVO.class);
	        vo.setProductName(batch.getProduct().getProductName());
	        vo.setProductCode(batch.getProduct().getProductCode());
	        expiryItems.add(vo);
	    }
	    return expiryItems;
	}
	
}
