package com.ims.service;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ims.dto.TransactionVO;
import com.ims.entity.TransactionEntity;
import com.ims.entity.UserEntity;
import com.ims.exception.StoreNotFoundException;
import com.ims.exception.UserNotFoundException;
import com.ims.repository.TransactionRepository;
import com.ims.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ModelMapper modelMapper;
	
	@Autowired
	private TransactionRepository transactionRepository;

	@Override
	public List<TransactionVO> getTransactionHistory(String username) {
		log.info("In getTransactionHistory "+username);
	    UserEntity user = userRepository.findByUsername(username);
	    if (user == null) {
	    	log.error("User not found "+user);
	        throw new UserNotFoundException("User not found");
	    } 
	    
	    if(user.getStore() == null) {
	    	log.error("Store not found "+user.getStore());
	        throw new StoreNotFoundException("Store not found");
	    }

	    List<TransactionEntity> transactions = transactionRepository.findAllByStore(user.getStore());
	    
	    List<TransactionVO> transactionVOList = new ArrayList<>();
	    for (TransactionEntity transaction : transactions) {
	        TransactionVO vo = modelMapper.map(transaction, TransactionVO.class);
	        
            vo.setProductCode(transaction.getProduct().getProductCode());
            vo.setProductName(transaction.getProduct().getProductName());
            vo.setCategory(transaction.getProduct().getCategory());
            vo.setDefaultUnits(transaction.getProduct().getDefaultUnits());
	       
	        transactionVOList.add(vo);
	    }
	    
	    return transactionVOList;
	}
	
}
