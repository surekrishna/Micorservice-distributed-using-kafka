package com.stock.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.dto.DeliveryEvent;
import com.stock.dto.PaymentEvent;
import com.stock.dto.StockDto;
import com.stock.entity.WareHouse;
import com.stock.repository.StockRepository;
import com.stock.service.StockService;

@Service
public class StockServiceImpl implements StockService {

	@Autowired
	private StockRepository stockRepository;

	@Autowired
	private KafkaTemplate<String, PaymentEvent> kafkaPaymentTemplate;

	@Autowired
	private KafkaTemplate<String, DeliveryEvent> kafkaDelieveryTemplate;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public void createStock(StockDto stockDto) throws Exception {
		Iterable<WareHouse> items = stockRepository.findByItem(stockDto.getItem());

		if (items.iterator().hasNext()) {
			items.forEach(stock -> {
				stock.setQuantity(stockDto.getQuantity() + stock.getQuantity());
				stockRepository.save(stock);
			});
		} else {
			WareHouse wareHouse = new WareHouse();
			wareHouse.setItem(stockDto.getItem());
			wareHouse.setQuantity(stockDto.getQuantity());
			stockRepository.save(wareHouse);
		}

	}

	@Override
	@KafkaListener(topics = "NEW-PAYMENT", groupId = "PAYMENT-GROUP")
	public void updateStock(String event) throws Exception {
		PaymentEvent paymentEvent = objectMapper.readValue(event, PaymentEvent.class);
		try {
			Iterable<WareHouse> items = stockRepository.findByItem(paymentEvent.getOrderDto().getItem());

			if (!items.iterator().hasNext()) {
				System.out.println("There are no items, reverting the order");
				throw new Exception("Stocks are not avilable!");
			}

			items.forEach(item -> {
				item.setQuantity(item.getQuantity() - paymentEvent.getOrderDto().getQuantity());
				stockRepository.save(item);
			});

			DeliveryEvent deliverEvent = new DeliveryEvent();
			deliverEvent.setType("STOCK-UPDATE");
			deliverEvent.setOrderDto(paymentEvent.getOrderDto());
			kafkaDelieveryTemplate.send("NEW-STOCK", deliverEvent);
		} catch (Exception e) {
			paymentEvent.setType("PAYMENT-REVERSE");
			kafkaPaymentTemplate.send("REVERSE-PAYMENT", paymentEvent);
		}

	}

	@Override
	@KafkaListener(topics = "REVERSE-STOCK", groupId = "STOCK-GROUP")
	public void reverseStock(String event) throws Exception {
		DeliveryEvent deliveryEvent = objectMapper.readValue(event, DeliveryEvent.class);
		
		try {
			Iterable<WareHouse> items = stockRepository.findByItem(deliveryEvent.getOrderDto().getItem());
			
			items.forEach(item -> {
				item.setQuantity(item.getQuantity() + deliveryEvent.getOrderDto().getQuantity());
				stockRepository.save(item);
			});
			
			PaymentEvent paymentEvent = new PaymentEvent();
			paymentEvent.setOrderDto(deliveryEvent.getOrderDto());
			paymentEvent.setType("PAYMENT-REVERSE");
			
			kafkaPaymentTemplate.send("REVERSE-PAYMENT", paymentEvent);
		} catch (Exception e) {
			
		}
		
	}

}
