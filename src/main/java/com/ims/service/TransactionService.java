package com.ims.service;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ims.dto.TransactionVO;
import com.ims.entity.ProductEntity;
import com.ims.entity.TransactionEntity;
import com.ims.entity.UserEntity;
import com.ims.repository.TransactionRepository;
import com.ims.repository.UserRepository;

@Service
public class TransactionService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private TransactionRepository transactionRepository;

	public List<TransactionVO> getTransactionHistory(String username) {
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null || user.getStore() == null) {
	        throw new RuntimeException("Store not found");
	    } 

	    List<TransactionEntity> transactions = transactionRepository.findAllByStore(user.getStore());
	    
	    List<TransactionVO> transactionVOList = new ArrayList<>();
	    for (TransactionEntity transaction : transactions) {
	        TransactionVO vo = modelMapper.map(transaction, TransactionVO.class);
	        
	        vo.setProductCode(transaction.getProduct().getProductCode());
	        vo.setProductName(transaction.getProduct().getProductName());
	        vo.setCategory(transaction.getProduct().getCategory());
	        
	        transactionVOList.add(vo);
	    }
	    
	    return transactionVOList;
	}
	
}
