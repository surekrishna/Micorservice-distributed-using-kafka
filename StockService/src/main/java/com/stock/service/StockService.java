package com.stock.service;

import com.stock.dto.StockDto;

public interface StockService {

	void createStock(StockDto stockDto) throws Exception;

	void updateStock(String event) throws Exception;

	void reverseStock(String event) throws Exception;
}
