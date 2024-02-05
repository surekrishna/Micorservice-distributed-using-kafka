package com.payment.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.dao.OrderDto;
import com.payment.dao.OrderEvent;
import com.payment.dao.PaymentDto;
import com.payment.dao.PaymentEvent;
import com.payment.entity.Payment;
import com.payment.repository.PaymentRepository;
import com.payment.service.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private KafkaTemplate<String, PaymentEvent> paymentKafkaTemplate;

	@Autowired
	private KafkaTemplate<String, OrderEvent> orderKafkaTemplate;

	@Override
	@KafkaListener(topics = "NEW-ORDER", groupId = "ORDER-GROUP")
	public void processPayment(String event) throws Exception {
		Payment payment = null;
		OrderEvent orderEvent = objectMapper.readValue(event, OrderEvent.class);
		if (null == orderEvent)
			return;

		payment = new Payment();
		payment.setAmount(orderEvent.getOrderDto().getAmount());
		payment.setMode(orderEvent.getOrderDto().getPaymentMethod());
		payment.setOrderId(orderEvent.getOrderDto().getOrderId());
		payment.setStatus("SUCCESS");

		paymentRepository.save(payment);

		PaymentDto paymentDto = new PaymentDto();
		paymentDto.setAmount(payment.getAmount());
		paymentDto.setId(payment.getId());
		paymentDto.setMode(payment.getMode());
		paymentDto.setOrderId(payment.getOrderId());
		paymentDto.setStatus(payment.getStatus());

		PaymentEvent paymentEvent = new PaymentEvent();
		paymentEvent.setPaymentDto(paymentDto);
		paymentEvent.setOrderDto(orderEvent.getOrderDto());
		paymentEvent.setType("PAYMENT-CREATED");

		try {
			paymentKafkaTemplate.send("NEW-PAYMENT", paymentEvent);
		} catch (Exception e) {
			if (null != payment && payment.getId() != null) {
				payment.setStatus("FAILED");
				paymentRepository.save(payment);

				OrderEvent orderEventFailed = new OrderEvent();
				orderEventFailed.setOrderDto(orderEvent.getOrderDto());
				orderEventFailed.setType("ORDER-REVERSE");
				orderKafkaTemplate.send("REVERSE-ORDER", orderEventFailed);
			}
		}
	}

	@Override
	@KafkaListener(topics = "REVERSE-PAYMENT", groupId = "PAYMENT-GROUP")
	public void reversePayment(String event) throws Exception {
		PaymentEvent paymentEvent = new ObjectMapper().readValue(event, PaymentEvent.class);
		if (null == paymentEvent)
			return;

		Optional<Payment> payment = paymentRepository.findById(paymentEvent.getPaymentDto().getId());
		payment.ifPresent(pay -> {
			pay.setStatus("FAILED");
			paymentRepository.save(pay);
			
			OrderEvent orderEvent = new OrderEvent();
			OrderDto orderDto = new OrderDto();
			orderDto.setOrderId(pay.getOrderId());
			orderEvent.setOrderDto(orderDto);
			
			orderKafkaTemplate.send("REVERSE-ORDER", orderEvent);
		});

	}

}
