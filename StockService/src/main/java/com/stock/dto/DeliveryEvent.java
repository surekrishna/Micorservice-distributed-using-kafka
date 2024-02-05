package com.stock.dto;

public class DeliveryEvent {

	private String type;
	private OrderDto orderDto;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public OrderDto getOrderDto() {
		return orderDto;
	}

	public void setOrderDto(OrderDto orderDto) {
		this.orderDto = orderDto;
	}

}
