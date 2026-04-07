package com.ims.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
import com.ims.exception.BatchNumberExistsException;
import com.ims.exception.EmailFailedException;
import com.ims.exception.LessQuantityException;
import com.ims.exception.ObjectOptimisticLockingFailureException;
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
	private BatchRepository batchRepository;
	
	@Autowired
	private TransactionRepository transactionRepository;
	
	@Autowired
	private NotificationService notificationService;
	
	@Transactional
	public String addProduct(ProductDto productDto, String username) {
		log.info("In addProduct service "+productDto+" "+username);
		
		UserEntity userEntity = userRepository.findByUsername(username);
		
		if (userEntity == null) {
			log.error("User not found "+username);
            throw new UserNotFoundException("User Not Found");
        }
		
		if (productRepository.existsByProductCodeAndStoreAndActiveTrue(productDto.getProductCode(), userEntity.getStore())) {
			log.error("A product with this code already exists in your store, "+productDto);
            throw new ProductCodeAlreadyExistsInStoreException("A product with this code already exists in your store.");
        }
		
		if (productRepository.existsByProductNameAndStoreAndActiveTrue(productDto.getProductName().toUpperCase(), userEntity.getStore())) {
			log.error("A product named '" + productDto.getProductName() + "' is already registered. Please use a unique name or add a variant (e.g., 'Blue', 'XL').");
	        throw new ProductNameAlreadyExistsInStore("A product named '" + productDto.getProductName() + "' is already registered. Please use a unique name or add a variant (e.g., 'Blue', 'XL').");
	    }
		
		ProductEntity productEntity = modelMapper.map(productDto, ProductEntity.class);
		
		productEntity.setStore(userEntity.getStore());
		productEntity.setProductName(productEntity.getProductName().toUpperCase());
		productEntity.setTotalQuantity(0.0);
		productEntity.setTotalPurchasePrice(0.0);
		
		userEntity.getStore().getProducts().add(productEntity);
		
		productRepository.save(productEntity);
		
		try {
            notificationService.sendProductAdditionNotification(userEntity, productEntity);
            log.info("Notification send to add product "+productEntity+" from "+userEntity);
        } catch (EmailFailedException e) {
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
		List<String> categories = productRepository.findDistinctCategoriesByStoreAndActiveTrue(store);
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
		return productRepository.findProductsByStoreAndCategoryAndActiveTrue(userEntity.getStore(), categoryName);
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
        
        if(batchRepository.existsByBatchNumberAndProductStore(dto.getBatchNumber(),user.getStore())) {
        	log.error("Batch is already exists "+dto.getBatchNumber());
        	throw new BatchNumberExistsException("Batch number already exists in store");
        }

        ProductEntity product = productRepository.findByProductNameAndStoreAndActiveTrue(dto.getProductName().toUpperCase(), user.getStore());
        
        if (product == null) {
        	log.error("Product " + dto.getProductName() + " not found.");
            throw new ProductNotFoundException("Product " + dto.getProductName() + " not found. Please add the product first.");
        }

        BatchEntity batch = modelMapper.map(dto, BatchEntity.class);
        batch.setPurchasePrice(dto.getPurchasePrice()*dto.getCurrentQuantity());
        batch.setProduct(product);
        batchRepository.save(batch);

        product.setTotalQuantity(product.getTotalQuantity()+dto.getCurrentQuantity());
        product.setTotalPurchasePrice(product.getTotalPurchasePrice()+(dto.getPurchasePrice()*dto.getCurrentQuantity()));

        TransactionEntity transaction = new TransactionEntity();
        transaction.setProduct(product);
        transaction.setType(TransactionType.PURCHASE);
        transaction.setQuantity(dto.getCurrentQuantity());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setPartyName(dto.getPartyName());
        transaction.setTotalAmount(dto.getCurrentQuantity()*dto.getPurchasePrice()); 
        
        transactionRepository.save(transaction);
        
        product.getTransactions().add(transaction);
        
        productRepository.save(product);
        
        UserEntity owner = user.getStore().getOwner();
        
        try {
            notificationService.sendBatchAdditionNotification(owner, product, batch);
            log.info("Notification send of batch Addition "+user+ " product "+product+" batch "+batch);
        } catch (EmailFailedException e) {
        	log.error("Failed to send welcome email: " + e.getMessage());
        }

        return "Purchase done Successfully";
    }

	@Transactional
	public String sellProduct(String username, SaleDto saleDto) {
	    log.info("In sellProduct service {} {}", username, saleDto);

	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) throw new UserNotFoundException("User not found");
	    if (user.getStore() == null) throw new StoreNotFoundException("Store not found");

	    ProductEntity productEntity = productRepository.findByProductNameAndCategoryAndStoreAndActiveTrueWithLock(
	            saleDto.getProductName().toUpperCase(), saleDto.getCategory(), user.getStore());

	    productEntity.getBatches().size();//to handle concurrency control, to not use cached batches
	    
	    if (productEntity.getTotalQuantity() < saleDto.getQuantity()) {
	        log.error("Insufficient stock for {}. Available: {}", productEntity.getProductName(), productEntity.getTotalQuantity());
	        throw new LessQuantityException("Quantity is less, total Quantity of " + productEntity.getProductName() + " is " + productEntity.getTotalQuantity());
	    }
	    
	    if (productEntity.getTotalQuantity() <= 0) {
	        throw new LessQuantityException("No stock available to calculate price");
	    }

	    Double individualProductPrice = productEntity.getTotalPurchasePrice() / productEntity.getTotalQuantity();
	    
	    Double totalCostOfSoldItems = round(individualProductPrice * saleDto.getQuantity());

	    Double totalRevenue = round(saleDto.getMrp() * saleDto.getQuantity());

	    List<BatchEntity> batches = productEntity.getBatches();
	    batches.sort(Comparator.comparing(BatchEntity::getExpiryDate, 
	                 Comparator.nullsLast(Comparator.naturalOrder())));

	    Double tempQuantity = saleDto.getQuantity();

	    Iterator<BatchEntity> iterator = batches.iterator();
	    while (tempQuantity > 0.001 && iterator.hasNext()) {
	        BatchEntity batch = iterator.next();
	        
	        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(LocalDate.now())) {
	            log.warn("Skipping expired batch: {} for product: {}", batch.getBatchNumber(), productEntity.getProductName());
	            continue;
	        }

	        if (batch.getCurrentQuantity() <= tempQuantity + 0.001) {
	            tempQuantity -= batch.getCurrentQuantity();
	            iterator.remove();
	            batchRepository.delete(batch);
	        } else {
	            batch.setCurrentQuantity(round(batch.getCurrentQuantity() - tempQuantity));
	            tempQuantity = 0.0;
	            batchRepository.save(batch);
	        }
	    }

	    productEntity.setTotalPurchasePrice(round(productEntity.getTotalPurchasePrice() - totalCostOfSoldItems));
	    productEntity.setTotalQuantity(round(productEntity.getTotalQuantity() - saleDto.getQuantity()));

	    TransactionEntity transaction = new TransactionEntity();
	    transaction.setProduct(productEntity);
	    transaction.setType(TransactionType.SALE);
	    transaction.setQuantity(saleDto.getQuantity()); 
	    transaction.setTransactionDate(LocalDateTime.now());
	    transaction.setPartyName(saleDto.getCustomerName());
	    transaction.setTotalAmount(totalRevenue);

	    transactionRepository.save(transaction);
	    productEntity.getTransactions().add(transaction);
	    productRepository.save(productEntity);
	    
	    UserEntity owner = user.getStore().getOwner();
	    
	    if (productEntity.getTotalQuantity() <= productEntity.getMinStockLevel()) {
	        try {
	        	log.info("In sendLowQuantityNotification " +productEntity.getTotalQuantity()+ " "+productEntity.getMinStockLevel());
	            notificationService.sendLowQuantityNotification(owner, productEntity);
	        	log.info("In sendLowQuantityNotification Email send" +productEntity.getTotalQuantity()+ " "+productEntity.getMinStockLevel());
	        } catch (EmailFailedException e) {
	            log.error("Failed to send low stock notification: " + e.getMessage());
	        }
	    }

	    try {
	        notificationService.sendSaleNotification(owner, productEntity, saleDto);
	    } catch (EmailFailedException e) {
	        log.error("Failed to send email: " + e.getMessage());
	    }

	    return "Sale successful.";
	}

	private Double round(Double value) {
	    if (value == null) return 0.0;
	    return Math.round(value * 100.0) / 100.0;
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
	    	if(product.isActive()) {
	    		 ProductVO productVO = modelMapper.map(product, ProductVO.class);
	 	      
	 	        listProductVO.add(productVO);
	    	}
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

	    ProductEntity productEntity = productRepository.findByProductCodeAndStoreAndActiveTrue(productEditDto.getProductCode(), user.getStore());
	    
	    if (productEntity == null) {
	    	log.error("Product not found with code: " + productEditDto.getProductCode());
	        throw new ProductNotFoundException("Product not found with code: " + productEditDto.getProductCode());
	    }
	    
	    String oldName = productEntity.getProductName();
	    String oldCategory = productEntity.getCategory();
	    Double oldMinStock = productEntity.getMinStockLevel();

	    ProductEntity duplicateNameProduct = productRepository.findByProductNameAndStoreAndActiveTrue(productEditDto.getProductName().toUpperCase(), user.getStore());
	        
	    if (duplicateNameProduct != null && !duplicateNameProduct.getId().equals(productEntity.getId())) {
	    	log.error("Product name '" + productEditDto.getProductName() + "' is already used by another product.");
	        throw new ProductNameAlreadyExistsInStore("Product name '" + productEditDto.getProductName() + "' is already used by another product.");
	    }

	    productEntity.setCategory(productEditDto.getCategory());
	    productEntity.setMinStockLevel(productEditDto.getMinStockLevel());
	    productEntity.setProductName(productEditDto.getProductName().toUpperCase());
	    
	    productRepository.save(productEntity);
	    
	    try {
	        notificationService.sendProductUpdateNotification(user, oldName, oldCategory, oldMinStock, productEditDto);
	        log.info("Update notification sent for product: {}", oldName);
	    } catch (EmailFailedException e) {
	        log.error("Failed to send product update email for user: {} and product: {}. Error: {}", username, oldName, e.getMessage());
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
        ProductEntity productEntity = productRepository.findByProductCodeAndStoreAndActiveTrue(productCode, user.getStore());	    
        if (productEntity == null) {
	    	log.error("Product not found with code: " +productCode);
	        throw new ProductNotFoundException("Product not found with code: " +productCode);
	    }
        String productName = productEntity.getProductName();
	    
	    List<BatchEntity> batches = productEntity.getBatches();
	    if (!batches.isEmpty()) {
	        batchRepository.deleteAll(batches); 
	        productEntity.getBatches().clear(); 
	    }
	    
	    productEntity.setActive(false);
	    productEntity.setTotalPurchasePrice(0.0);
	    productEntity.setTotalQuantity(0.0);

	    productRepository.save(productEntity);
	    
	    try {
	        notificationService.sendProductDeactivationNotification(user, productName );
	        log.info("Deactivation email sent for product: {} to user: {}", productName, username);
	    } catch (EmailFailedException e) {
	        log.error("Failed to send deactivation email for {} [User: {}]. Error: {}", productName, username, e.getMessage());
	    }
	    
		return "Product Deactivated successfully";
	} 
	
	@Transactional
	public String updateBatch(String username, BatchUpdateDto dto) {
	    log.info("In updateBatch service: {} | {}", username, dto);

	    UserEntity user = userRepository.findByUsername(username);

	    BatchEntity batch = batchRepository.findById(dto.getId())
	            .orElseThrow(() -> new BatchNotFoundException("Batch not found with ID: " + dto.getId()));

	    if (!batch.getProduct().getStore().getId().equals(user.getStore().getId())) {
	        throw new BatchDoesNotBelongToUserException("This batch does not belong to your store.");
	    }

	    LocalDate oldExpiry = batch.getExpiryDate();
	    String oldLocation = batch.getLocation();
	    
	    ProductEntity product = batch.getProduct();

	    batch.setExpiryDate(dto.getExpiryDate());
	    batch.setLocation(dto.getLocation());
	    
	    try {
	        batchRepository.save(batch);
	    } catch (ObjectOptimisticLockingFailureException e) {
	        throw new ObjectOptimisticLockingFailureException("This batch was modified by another user. Please refresh and try again.");
	    }

	    try {
	        notificationService.sendBatchUpdateNotification(user, product, oldExpiry, oldLocation, dto);
	    } catch (EmailFailedException e) {
	        log.error("Failed to notify user {} about batch update", user.getUsername());
	    }
	    
	    return "Batch updated successfully.";
	}
	
	@Transactional
	public String deleteBatch(String username, Long id) {
	    log.info("Deleting batch ID: {} for user: {}", id, username);

	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) throw new UserNotFoundException("User not found");

	    BatchEntity batch = batchRepository.findById(id)
	            .orElseThrow(() -> new BatchNotFoundException("Batch not found with ID: " + id));

	    ProductEntity product = batch.getProduct();
	    
	    if (!product.getStore().getId().equals(user.getStore().getId())) {
	        throw new BatchDoesNotBelongToUserException("Access denied to this batch.");
	    }

	    String deletedBatchNo = batch.getBatchNumber();
	    String productName = product.getProductName();

	    product.setTotalQuantity(round(product.getTotalQuantity() - batch.getCurrentQuantity()));
	    product.setTotalPurchasePrice(round(product.getTotalPurchasePrice() - batch.getPurchasePrice()));

	    product.getBatches().remove(batch);
	    
	    try {
	    	batchRepository.delete(batch);
		    productRepository.save(product);
	    }catch (ObjectOptimisticLockingFailureException e) {
	        throw new ObjectOptimisticLockingFailureException("This batch was modified by another user. Please refresh and try again.");
	    }
	    
	    try {
	        notificationService.sendBatchDeletionNotification(user, productName, deletedBatchNo, product);
	        log.info("Batch deletion notification sent for: {}", deletedBatchNo);
	    } catch (Exception e) {
	        log.error("Email failed for batch deletion {}: {}", deletedBatchNo, e.getMessage());
	    }

	    return "Batch " + deletedBatchNo + " deleted successfully.";
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
	    	if(product.isActive() && product.getTotalQuantity()<product.getMinStockLevel()) {
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
		UserEntity owner = user.getStore().getOwner();

        if(user.getStore() == null) {
        	log.error("Store not found "+username);
        	throw new StoreNotFoundException("Store not found");
        }
        
	    LocalDate today = LocalDate.now();
	    LocalDate thirtyDaysFromNow = today.plusDays(30);
	    List<BatchEntity> batches = batchRepository.findExpiringBatches(owner.getUsername(), today, thirtyDaysFromNow);
	    List<BatchExpiryVO> expiryItems = new ArrayList<>();
	    for (BatchEntity batch : batches) {
	        BatchExpiryVO vo = modelMapper.map(batch, BatchExpiryVO.class);
	        vo.setProductName(batch.getProduct().getProductName());
	        vo.setProductCode(batch.getProduct().getProductCode());
	        vo.setDefaultUnits(batch.getProduct().getDefaultUnits());
	        expiryItems.add(vo);
	    }
	    return expiryItems;
	}

	public String getProductUnit(String username, String productName) {
	    log.info("Fetching unit for product: {} for user: {}", productName, username);
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) {
	        log.error("User not found: {}", username);
	        throw new UserNotFoundException("User not found");
	    }

	    StoreEntity store = user.getStore();
	    if (store == null) {
	        log.error("Store not found for user: {}", username);
	        throw new StoreNotFoundException("Store not found for this account");
	    }
	  
	    ProductEntity product = productRepository.findByProductNameAndStoreAndActiveTrue(productName.toUpperCase(), store);

	    if (product == null) {
	        log.error("Product {} not found in store {}", productName, store.getStoreName());
	        throw new ProductNotFoundException("Product '" + productName + "' not found.");
	    }

	    return product.getDefaultUnits();
	}

	public boolean checkProductCodeExists(String username, String productCode) {
		log.info("In checkProductCodeExists service "+username+" searching product code "+productCode);
		UserEntity user = userRepository.findByUsername(username);
		if(user == null) {
			log.error("In checkProductCodeExists service "+username+" searching productcode "+productCode);
			throw new UserNotFoundException("User not found"); 
		}
		if(user.getStore() == null) {
			log.error("In checkProductCodeExists service "+username+" searching productcode "+productCode);
			throw new StoreNotFoundException("Store not found"); 
		}
		boolean isProductCodeExists = productRepository.existsByProductCodeAndStoreAndActiveTrue(productCode, user.getStore());
		return isProductCodeExists;
	}
	
	public String generateFiveDigitCode() {
		Random random = new Random();
	    int otp = 10000 + random.nextInt(90000); 
	    return String.valueOf(otp);
	}

	public boolean checkBatchNumberExists(String username, String batchNumber) {
		log.info("In checkBatchNumberExists service "+username+" searching batch number "+batchNumber);
		UserEntity user = userRepository.findByUsername(username);
		if(user == null) {
			log.error("In checkBatchNumberExists service "+username+" searching batch number "+batchNumber);
			throw new UserNotFoundException("User not found"); 
		}
		if(user.getStore() == null) {
			log.error("In checkBatchNumberExists service "+username+" searching batch number "+batchNumber);
			throw new StoreNotFoundException("Store not found"); 
		}
		boolean isBatchNumberExists = batchRepository.existsByBatchNumberAndProductStore(batchNumber, user.getStore());
		return isBatchNumberExists;
	}

}


