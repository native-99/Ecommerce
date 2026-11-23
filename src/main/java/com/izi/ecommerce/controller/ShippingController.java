package com.izi.ecommerce.controller;

import com.izi.ecommerce.model.ShippingOrderRequest;
import com.izi.ecommerce.model.ShippingOrderResponse;
import com.izi.ecommerce.model.ShippingRateRequest;
import com.izi.ecommerce.model.ShippingRateResponse;
import com.izi.ecommerce.model.UserInfo;
import com.izi.ecommerce.service.OrderService;
import com.izi.ecommerce.service.ShippingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("shippings")
@SecurityRequirement(name = "Bearer")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;
    private final OrderService orderService;

    @PostMapping("/rate")
    public ResponseEntity<ShippingRateResponse> calculateShippingRate(
            @Valid @RequestBody ShippingRateRequest shippingRateRequest) {
        ShippingRateResponse response = shippingService.calculateShippingRate(shippingRateRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/order")
    public ResponseEntity<ShippingOrderResponse> createShippingOrder(
            @Valid @RequestBody ShippingOrderRequest shippingOrderRequest) {
        ShippingOrderResponse response = shippingService.createShippingOrder(shippingOrderRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/awb/{orderId}")
    public ResponseEntity<String> generateAwbNumber(@PathVariable Long orderId) {
        orderService.findOrderOwnedByUser(orderId, getCurrentUserId());
        String awbNumber = shippingService.generateAwbNumber(orderId);
        return ResponseEntity.ok(awbNumber);
    }

    @GetMapping("/weight/{orderId}")
    public ResponseEntity<BigDecimal> calculateTotalWeight(@PathVariable Long orderId) {
        orderService.findOrderOwnedByUser(orderId, getCurrentUserId());
        BigDecimal totalWeight = shippingService.calculateTotalWeight(orderId);
        return ResponseEntity.ok(totalWeight);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) authentication.getPrincipal();
        return userInfo.getUser().getUserId();
    }
}