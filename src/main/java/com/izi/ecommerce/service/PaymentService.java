package com.izi.ecommerce.service;

import com.izi.ecommerce.entity.Order;
import com.izi.ecommerce.model.PaymentNotification;
import com.izi.ecommerce.model.PaymentResponse;

public interface PaymentService {

    PaymentResponse create(Order order);

    PaymentResponse findByPaymentId(String paymentId);

    boolean verifyByPaymentId(String paymentId);

    void handleNotification(PaymentNotification paymentNotification);
}