package com.ims.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ims.dto.AddBatchToProductDto;
import com.ims.dto.BatchUpdateDto;
import com.ims.dto.BatchVO;
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
        ProductEntity productEntity = productRepository.findByProductNameAndCategoryAndStore(saleDto.getProductName().toUpperCase(),saleDto.getCategory(), user.getStore());
		
        if(productEntity.getTotalQuantity()<saleDto.getQuantity()) {
        	throw new RuntimeException("Quantity is less, total Quantity of "+productEntity.getProductName()+" is "+productEntity.getTotalQuantity());
        }
        
        Double individualProductPrice = productEntity.getTotalPurchasePrice()/productEntity.getTotalQuantity();
        Double totalPriceWithQuantitySendByUser = individualProductPrice*saleDto.getQuantity();
        Double sellingPrice = totalPriceWithQuantitySendByUser + (totalPriceWithQuantitySendByUser*saleDto.getInterestRate()/100);
        
        List<BatchEntity> batches = productEntity.getBatches();
        
        Integer tempQuantity = saleDto.getQuantity();
        
        while(tempQuantity > 0 && !batches.isEmpty()) {
        	BatchEntity batch = batches.get(0);//for FIFO of batch
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
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
        
        return "Sale is done with selling price "+ sellingPrice+ " on interest rate "+ saleDto.getInterestRate();
	}

	public List<ProductVO> getAllProducts(String username) {
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null || user.getStore() == null) {
	        throw new RuntimeException("Store not found for this user");
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
	            productVO.setPerItemPrice(product.getTotalPurchasePrice() / product.getTotalQuantity());
	        } else {
	            productVO.setPerItemPrice(0.0); 
	        }	        productVO.setBatches(listBatchVO);
	        listProductVO.add(productVO);
	    }
	    
	    return listProductVO;
	}
	
	@Transactional
	public String updateProduct(ProductEditDto productEditDto, String username) {
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null || user.getStore() == null) {
	        throw new RuntimeException("Store not found for this user");
	    } 

	    ProductEntity productEntity = productRepository.findByProductCodeAndStore(productEditDto.getProductCode(), user.getStore());
	    
	    if (productEntity == null) {
	        throw new RuntimeException("Product not found with code: " + productEditDto.getProductCode());
	    }

	    ProductEntity duplicateNameProduct = productRepository.findByProductNameAndStore(productEditDto.getProductName().toUpperCase(), user.getStore());
	        
	    if (duplicateNameProduct != null && !duplicateNameProduct.getId().equals(productEntity.getId())) {
	        throw new RuntimeException("Product name '" + productEditDto.getProductName() + "' is already used by another product.");
	    }

	    productEntity.setCategory(productEditDto.getCategory());
	    productEntity.setMinStockLevel(productEditDto.getMinStockLevel());
	    productEntity.setProductName(productEditDto.getProductName().toUpperCase());
	    
	    productRepository.save(productEntity);
	    
	    return "Product Updated successfully";
	}

	@Transactional
	public String deleteProduct(String productCode, String username) {
		UserEntity user = userRepository.findByUsername(username);
	    if (user == null || user.getStore() == null) {
	        throw new RuntimeException("Store not found for this user");
	    } 
	    ProductEntity productEntity = productRepository.findByProductCodeAndStore(productCode, user.getStore());
	    
	    if (productEntity == null) {
	        throw new RuntimeException("Product not found with code: " +productCode);
	    }
	    
	    productRepository.delete(productEntity);
	    
		return "Product Deleted successfully";
	} 
	
	@Transactional
	public String updateBatch(String username, BatchUpdateDto dto) {
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null || user.getStore() == null) {
	        throw new RuntimeException("Store not found");
	    }

	    BatchEntity batch = batchRepository.findById(dto.getId())
	            .orElseThrow(() -> new RuntimeException("Batch not found with ID: " + dto.getId()));

	    if (!batch.getProduct().getStore().getId().equals(user.getStore().getId())) {
	        throw new RuntimeException("Unauthorized: This batch does not belong to your store.");
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

	    return "Batch " + batch.getBatchNumber() + " updated successfully.";
	}
	
	@Transactional
	public String deleteBatch(String username, Long id) {
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null || user.getStore() == null) {
	        throw new RuntimeException("Store not found");
	    }

	    BatchEntity batch = batchRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("Batch not found with ID: " + id));

	    ProductEntity product = batch.getProduct();
	    if (!product.getStore().getId().equals(user.getStore().getId())) {
	        throw new RuntimeException("Unauthorized: Access denied to this batch.");
	    }

	    product.setTotalQuantity(product.getTotalQuantity() - batch.getCurrentQuantity());
	    product.setTotalPurchasePrice(product.getTotalPurchasePrice() - batch.getPurchasePrice());

	    product.getBatches().remove(batch);
	    batchRepository.delete(batch);
	    
	    productRepository.save(product);

	    return "Batch " + batch.getBatchNumber() + " deleted.";
	}
	
}
