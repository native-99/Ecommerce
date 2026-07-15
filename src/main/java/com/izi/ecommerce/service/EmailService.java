package com.izi.ecommerce.service;

import com.izi.ecommerce.entity.Order;

public interface EmailService {

    void notifySuccessfulPayment(Order order);
    void notifyUnsuccessfulPayment(Order order);


}
