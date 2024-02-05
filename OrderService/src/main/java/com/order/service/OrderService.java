package com.order.service;

import com.order.dao.OrderDto;
import com.order.entity.Order;

public interface OrderService {

	Order createOrder(OrderDto orderDto) throws Exception;

	Order updateOrder(OrderDto orderDto) throws Exception;

	Order getOrder(long id) throws Exception;

	void reverseOrder(String event) throws Exception;
}
