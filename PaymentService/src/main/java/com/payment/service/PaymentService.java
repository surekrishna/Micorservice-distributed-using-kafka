package com.payment.service;

public interface PaymentService {

	void processPayment(String event) throws Exception;
	void reversePayment(String event) throws Exception;
}
