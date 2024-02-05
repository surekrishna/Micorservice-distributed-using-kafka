package com.order.service.imp;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.dao.OrderDto;
import com.order.dao.OrderEvent;
import com.order.entity.Order;
import com.order.repository.OrderRepository;
import com.order.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public Order createOrder(OrderDto orderDto) throws Exception {
		Order order = new Order();
		order.setAmount(orderDto.getAmount());
		order.setItem(orderDto.getItem());
		order.setQuantity(orderDto.getQuantity());
		order.setStatus("CREATED");

		return orderRepository.save(order);
	}

	@Override
	public Order updateOrder(OrderDto orderDto) throws Exception {
		Order order = orderRepository.findById(orderDto.getOrderId()).orElse(null);
		if (null == order) {
			return order;
		}

		order.setAmount(orderDto.getAmount());
		order.setItem(orderDto.getItem());
		order.setQuantity(orderDto.getQuantity());
		order.setStatus(orderDto.getStatus());

		return orderRepository.save(order);
	}

	@Override
	public Order getOrder(long id) throws Exception {
		return orderRepository.findById(id).orElse(null);
	}

	@Override
	@KafkaListener(topics = "REVERSE-ORDER", groupId = "ORDER-GROUP")
	public void reverseOrder(String event) throws Exception {
		OrderEvent orderEvent = objectMapper.readValue(event, OrderEvent.class);
		Optional<Order> order = orderRepository.findById(orderEvent.getOrderDto().getOrderId());
		order.ifPresent(or -> {
			or.setStatus("FAILED");
			orderRepository.save(or);
		});
	}

}
