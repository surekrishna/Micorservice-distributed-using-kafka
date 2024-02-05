package com.delivery.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.delivery.dto.DeliveryEvent;
import com.delivery.entity.Delivery;
import com.delivery.repository.DeliveryRepository;
import com.delivery.service.DeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DeliveryServiceImpl implements DeliveryService {

	@Autowired
	private DeliveryRepository deliveryRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private KafkaTemplate<String, DeliveryEvent> kafkaDelieveryTemplate;

	@Override
	@KafkaListener(topics = "NEW-STOCK", groupId = "STOCK-GROUP")
	public void deliverOrder(String event) throws Exception {
		DeliveryEvent deliveryEvent = objectMapper.readValue(event, DeliveryEvent.class);
		Delivery delivery = null;
		try {
			if (deliveryEvent.getOrderDto().getAddress() == null) {
				throw new Exception("Address is not present!");
			}

			delivery = new Delivery();
			delivery.setAddress(deliveryEvent.getOrderDto().getAddress());
			delivery.setOrderId(deliveryEvent.getOrderDto().getOrderId());
			delivery.setStatus("SUCCESS");

			deliveryRepository.save(delivery);

		} catch (Exception e) {
			delivery.setStatus("FAILED");
			deliveryRepository.save(delivery);

			deliveryEvent.setType("STOCK-REVERSE");

			kafkaDelieveryTemplate.send("REVERSE-STOCK", deliveryEvent);
		}

	}

}
