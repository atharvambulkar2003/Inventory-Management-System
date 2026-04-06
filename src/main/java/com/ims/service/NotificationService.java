package com.ims.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.ims.dto.BatchUpdateDto;
import com.ims.dto.ProductEditDto;
import com.ims.dto.SaleDto;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;


@Service
public class NotificationService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateEngine templateEngine;

    public void sendOwnerWelcomeNotification(UserEntity user, StoreEntity store) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("storeName", store.getStoreName());
        
        String htmlContent = templateEngine.process("signup", context);
        emailService.sendHtmlEmail(user.getEmail(), "Welcome to IMS!", htmlContent);
    }
    
    public void sendLoginAlert(UserEntity user) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
        context.setVariable("loginTime", formatter.format(new java.util.Date()));
        
        String htmlContent = templateEngine.process("login-alert", context);
        emailService.sendHtmlEmail(user.getEmail(), "Security Alert: Login Detected", htmlContent);
    }
    
    public void sendOtpNotification(UserEntity user, String generatedOtp) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("otp", generatedOtp);
        
        String htmlContent = templateEngine.process("otp-email", context);        
        emailService.sendHtmlEmail(user.getEmail(), "Your Verification Code - IMS", htmlContent);
    }
    
    public void sendProfileUpdateNotification(UserEntity user) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());        
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
        context.setVariable("updateTime", formatter.format(new java.util.Date()));
        
        String htmlContent = templateEngine.process("profile-updated", context);
        emailService.sendHtmlEmail(user.getEmail(), "Success: Profile Updated - IMS", htmlContent);
    }
    
    public void sendPasswordOtp(UserEntity user, String generatedOtp) {
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("otp", generatedOtp);
        
        String htmlContent = templateEngine.process("password-otp", context);        
        emailService.sendHtmlEmail(user.getEmail(), "Security Alert: Password Change OTP", htmlContent);
    }
    
    public void sendPasswordUpdateConfirmation(UserEntity user) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy 'at' hh:mm a");
        context.setVariable("updateTime", formatter.format(new java.util.Date()));
        
        String htmlContent = templateEngine.process("password-updated", context);        
        emailService.sendHtmlEmail(user.getEmail(), "Security Alert: Password Updated Successfully", htmlContent);
    }
    
    public void forgotPasswordOtp(UserEntity user, String generatedOtp) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("otp", generatedOtp);
        
        String htmlContent = templateEngine.process("forget-password-otp", context);        
        emailService.sendHtmlEmail(user.getEmail(), "Security Alert: Password Change OTP", htmlContent);
    }
    
    public void sendPasswordOtpForEmail(UserEntity user, String generatedOtp) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("username", user.getUsername());
        context.setVariable("otp", generatedOtp);
        
        String htmlContent = templateEngine.process("password-change-email-otp", context);        
        emailService.sendHtmlEmail(user.getEmail(), "Security Alert: Password Change OTP", htmlContent);
    }
    
    public void sendStaffOnboardingNotifications(UserEntity owner, UserEntity staff, String plainPassword) {
        String storeName = owner.getStore().getStoreName();
        Context staffCtx = new Context();
        staffCtx.setVariable("staffName", staff.getFullName());
        staffCtx.setVariable("storeName", storeName);
        staffCtx.setVariable("username", staff.getUsername());
        staffCtx.setVariable("password", plainPassword);
        
        String staffHtml = templateEngine.process("staff-welcome", staffCtx);
        emailService.sendHtmlEmail(staff.getEmail(), "Welcome to the Team at " + storeName, staffHtml);

        Context ownerCtx = new Context();
        ownerCtx.setVariable("ownerName", owner.getFullName());
        ownerCtx.setVariable("staffName", staff.getFullName());
        ownerCtx.setVariable("storeName", storeName);
        
        String ownerHtml = templateEngine.process("staff-added-owner", ownerCtx);
        emailService.sendHtmlEmail(owner.getEmail(), "Staff Added Successfully", ownerHtml);
    }
    
    public void sendProductAdditionNotification(UserEntity user, ProductEntity product) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("productName", product.getProductName());
        context.setVariable("productCode", product.getProductCode());
        context.setVariable("category", product.getCategory());
        context.setVariable("minStock", product.getMinStockLevel());

        String html = templateEngine.process("product-added", context);
        emailService.sendHtmlEmail(user.getEmail(), "New Product: " + product.getProductName(), html);
    }

    public void sendBatchAdditionNotification(UserEntity user, ProductEntity product, BatchEntity batch) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("productName", product.getProductName());
        context.setVariable("batchNo", batch.getBatchNumber());
        context.setVariable("batchQty", batch.getCurrentQuantity());
        context.setVariable("totalQty", product.getTotalQuantity());

        String html = templateEngine.process("batch-added", context);
        emailService.sendHtmlEmail(user.getEmail(), "Stock In Alert: Batch " + batch.getBatchNumber(), html);
    }

    public void sendSaleNotification(UserEntity user, ProductEntity product, SaleDto saleDto) {
        Context context = new Context();
        context.setVariable("productName", product.getProductName());
        context.setVariable("soldQty", saleDto.getQuantity());
        context.setVariable("customer", saleDto.getCustomerName());
        context.setVariable("remainingQty", product.getTotalQuantity());

        String html = templateEngine.process("sale-report", context);
        emailService.sendHtmlEmail(user.getEmail(), "Sale Confirmed: " + product.getProductName(), html);
    }
    
    public void sendProductUpdateNotification(UserEntity user, String oldName, String oldCategory, double oldMinStock, ProductEditDto newDto) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("oldName", oldName);
        context.setVariable("oldCategory", oldCategory);
        context.setVariable("oldMinStock", oldMinStock);
        context.setVariable("newName", newDto.getProductName().toUpperCase());
        context.setVariable("newCategory", newDto.getCategory());
        context.setVariable("newMinStock", newDto.getMinStockLevel());

        String htmlContent = templateEngine.process("product-updated", context);
        emailService.sendHtmlEmail(user.getEmail(), "Product Update Successful: " + oldName, htmlContent);
    }
    
    public void sendProductDeactivationNotification(UserEntity user, String productName) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("productName", productName);

        String htmlContent = templateEngine.process("product-deactivated", context);
        emailService.sendHtmlEmail(user.getEmail(), "Product Deactivated: " + productName, htmlContent);
    }
    
    public void sendBatchUpdateNotification(UserEntity user, ProductEntity product, LocalDate oldExpiry, String oldLocation, BatchUpdateDto newDto) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        context.setVariable("productName", product.getProductName());       
        context.setVariable("oldExpiry", oldExpiry != null ? oldExpiry.toString() : "None");
        context.setVariable("newExpiry", newDto.getExpiryDate() != null ? newDto.getExpiryDate().toString() : "None");
        context.setVariable("oldLocation", oldLocation != null ? oldLocation : "Not Assigned");
        context.setVariable("newLocation", newDto.getLocation() != null ? newDto.getLocation() : "Not Assigned");
        
        String htmlContent = templateEngine.process("batch-updated", context);
        emailService.sendHtmlEmail(user.getEmail(), "Batch Info Updated: " + product.getProductName(), htmlContent);
    }
	
	public void sendBatchDeletionNotification(UserEntity user, String productName, String batchNo, ProductEntity product) {
		Context context = new Context();
		context.setVariable("fullName", user.getFullName());
		context.setVariable("productName", productName);
		context.setVariable("batchNo", batchNo);
		context.setVariable("totalQty", product.getTotalQuantity());
		context.setVariable("units", product.getDefaultUnits() != null ? product.getDefaultUnits() : "units");
		
		String html = templateEngine.process("batch-deleted", context);
		emailService.sendHtmlEmail(user.getEmail(), "Batch Deleted: " + batchNo, html);
	}

	public void sendLowQuantityNotification(UserEntity owner, ProductEntity productEntity) {
		Context context = new Context();
		context.setVariable("fullName",owner.getFullName());
		context.setVariable("productName", productEntity.getProductName());
		context.setVariable("productCode", productEntity.getProductCode());
		context.setVariable("productCategory", productEntity.getCategory());
		context.setVariable("productCurrentQuantity", productEntity.getTotalQuantity());
		context.setVariable("Units", productEntity.getDefaultUnits());
		context.setVariable("MinimumStockLevel", productEntity.getMinStockLevel());
		
		String html = templateEngine.process("lowstock-alert", context);
		emailService.sendHtmlEmail(owner.getEmail(), "Low stock alert : "+productEntity.getProductName(), html);
	}
    
    
}