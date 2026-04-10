package com.ims.service;

import java.time.LocalDate;

import com.ims.dto.BatchUpdateDto;
import com.ims.dto.ProductEditDto;
import com.ims.dto.SaleDto;
import com.ims.entity.BatchEntity;
import com.ims.entity.ProductEntity;
import com.ims.entity.StoreEntity;
import com.ims.entity.UserEntity;

public interface NotificationService {

	void sendOwnerWelcomeNotification(UserEntity user, StoreEntity store);

	void sendLoginAlert(UserEntity user);

	void sendOtpNotification(UserEntity user, String generatedOtp);

	void sendProfileUpdateNotification(UserEntity user);

	void sendPasswordOtp(UserEntity user, String generatedOtp);

	void sendPasswordUpdateConfirmation(UserEntity user);

	void forgotPasswordOtp(UserEntity user, String generatedOtp);

	void sendPasswordOtpForEmail(UserEntity user, String generatedOtp);

	void sendStaffOnboardingNotifications(UserEntity owner, UserEntity staff, String plainPassword);

	void sendProductAdditionNotification(UserEntity user, ProductEntity product);

	void sendBatchAdditionNotification(UserEntity user, ProductEntity product, BatchEntity batch);

	void sendSaleNotification(UserEntity user, ProductEntity product, SaleDto saleDto);

	void sendProductUpdateNotification(UserEntity user, String oldName, String oldCategory, double oldMinStock,
			ProductEditDto newDto);

	void sendProductDeactivationNotification(UserEntity user, String productName);

	void sendBatchUpdateNotification(UserEntity user, ProductEntity product, LocalDate oldExpiry, String oldLocation,
			BatchUpdateDto newDto);

	void sendBatchDeletionNotification(UserEntity user, String productName, String batchNo, ProductEntity product);

	void sendLowQuantityNotification(UserEntity owner, ProductEntity productEntity);

}