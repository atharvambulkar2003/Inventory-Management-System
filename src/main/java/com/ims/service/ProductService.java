package com.ims.service;

import java.util.Date;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ims.dto.ProductDto;
import com.ims.entity.ProductEntity;
import com.ims.entity.UserEntity;
import com.ims.repository.ProductRepository;
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
	
	@Transactional
	public String addProduct(ProductDto productDto, String username) throws Exception {
		UserEntity userEntity = userRepository.findByUsername(username);
		
		if (userEntity == null) {
            throw new RuntimeException("User not found");
        }
		
		if (productRepository.existsByProductCodeAndStore(productDto.getProductCode(), userEntity.getStore())) {
            throw new Exception("A product with this code already exists in your store.");
        }
		
		ProductEntity productEntity = modelMapper.map(productDto, ProductEntity.class);
		
		productEntity.setStore(userEntity.getStore());
		
		productEntity.setSellingPrice(0.0);
		
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

}
