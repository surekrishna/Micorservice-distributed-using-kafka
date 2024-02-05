package com.order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.order.dao.OrderDto;
import com.order.dao.OrderEvent;
import com.order.entity.Order;
import com.order.service.OrderService;

@RestController
@RequestMapping("/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Autowired
	private KafkaTemplate<String, OrderEvent> kafkaTemplate;

	@PostMapping("/create")
	public void createOrder(@RequestBody OrderDto orderDto) throws Exception {
		Order order = null;
		try {
			order = orderService.createOrder(orderDto);
			orderDto.setOrderId(order.getId());
			
			OrderEvent orderEvent = new OrderEvent();
			orderEvent.setOrderDto(orderDto);
			orderEvent.setType("ORDER-CREATED");

			kafkaTemplate.send("NEW-ORDER", orderEvent);
		} catch (Exception e) {
			if(null != order && order.getId() > 0) {
				orderDto.setOrderId(order.getId());
				orderDto.setStatus("FAILED");
				orderService.updateOrder(orderDto);	
			}			
		}
	}
}
