package com.izi.ecommerce.controller;


import com.izi.ecommerce.model.AddToCartRequest;
import com.izi.ecommerce.model.CartItemResponse;
import com.izi.ecommerce.model.UpdateCartItemRequest;
import com.izi.ecommerce.model.UserInfo;
import com.izi.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("carts")
@SecurityRequirement(name = "Bearer")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<Void> addItemToCart(
            @Valid @RequestBody AddToCartRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) auth.getPrincipal();

        cartService.addItemToCart(
                userInfo.getUser().getUserId(),
                request.getProductId(),
                request.getQuantity()
        );

        return ResponseEntity.ok().build();
    }

    @PutMapping("/items")
    public ResponseEntity<Void> updateCartItemQuantity(
            @Valid @RequestBody UpdateCartItemRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) auth.getPrincipal();

        cartService.updateCartItemQuantity(
                userInfo.getUser().getUserId(),
                request.getProductId(),
                request.getQuantity()
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/items")
    public ResponseEntity<List<CartItemResponse>> getCartItems() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) auth.getPrincipal();

        List<CartItemResponse> items =
                cartService.getCartItems(userInfo.getUser().getUserId());

        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable("id") Long cartItemId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) auth.getPrincipal();

        cartService.removeItemFromCart(
                userInfo.getUser().getUserId(),
                cartItemId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) auth.getPrincipal();

        cartService.clearCart(
                userInfo.getUser().getUserId());

        return ResponseEntity
                .noContent().build();
    }


}
