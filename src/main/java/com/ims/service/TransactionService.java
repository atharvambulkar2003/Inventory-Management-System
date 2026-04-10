package com.ims.service;

import java.util.List;

import com.ims.dto.TransactionVO;

public interface TransactionService {

	List<TransactionVO> getTransactionHistory(String username);

}