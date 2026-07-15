package com.izi.ecommerce.service;

import com.izi.ecommerce.model.ShippingOrderRequest;
import com.izi.ecommerce.model.ShippingOrderResponse;
import com.izi.ecommerce.model.ShippingRateRequest;
import com.izi.ecommerce.model.ShippingRateResponse;

import java.math.BigDecimal;

public interface ShippingService {

    ShippingRateResponse calculateShippingRate(ShippingRateRequest request);

    ShippingOrderResponse createShippingOrder(ShippingOrderRequest request);

    String generateAwbNumber(Long orderId);

    BigDecimal calculateTotalWeight(Long orderId);
}
