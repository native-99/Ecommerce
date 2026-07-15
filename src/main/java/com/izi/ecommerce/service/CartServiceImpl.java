package com.izi.ecommerce.service;

import com.izi.ecommerce.common.errors.BadRequestException;
import com.izi.ecommerce.common.errors.ForbiddenAccessException;
import com.izi.ecommerce.common.errors.InventoryException;
import com.izi.ecommerce.common.errors.ResourceNotFoundException;
import com.izi.ecommerce.entity.Cart;
import com.izi.ecommerce.entity.CartItem;
import com.izi.ecommerce.entity.Product;
import com.izi.ecommerce.model.CartItemResponse;
import com.izi.ecommerce.repository.CartItemRepository;
import com.izi.ecommerce.repository.CartRepository;
import com.izi.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void addItemToCart(Long userId, Long productId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });

        Product product = findProductByIdWithLock(productId);

        if (product.getUserId() != null && product.getUserId().equals(userId)) {
            throw new BadRequestException("Cannot add your own product to cart");
        }

        cartItemRepository.findByCartIdAndProductId(cart.getCartId(), productId)
                .ifPresentOrElse(
                        existingItem -> {
                            int newQuantity = existingItem.getQuantity() + quantity;
                            validateAvailableStock(product, newQuantity);
                            existingItem.setQuantity(newQuantity);
                            cartItemRepository.save(existingItem);
                        },
                        () -> {
                            validateAvailableStock(product, quantity);
                            CartItem newItem = CartItem.builder()
                                    .cartId(cart.getCartId())
                                    .productId(productId)
                                    .quantity(quantity)
                                    .price(product.getPrice())
                                    .build();
                            cartItemRepository.save(newItem);
                        }
                );
    }

    @Override
    @Transactional
    public void updateCartItemQuantity(Long userId, Long productId, int quantity) {
        Cart cart = findCartByUserId(userId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getCartId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product " + productId + " is not yet added to cart"
                ));

        if (quantity <= 0) {
            cartItemRepository.deleteById(item.getCartItemId());
            return;
        }

        Product product = findProductByIdWithLock(productId);
        validateAvailableStock(product, quantity);

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeItemFromCart(Long userId, Long cartItemId) {
        Cart cart = findCartByUserId(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCartId().equals(cart.getCartId())) {
            throw new ForbiddenAccessException("Cart item does not belong to user's cart");
        }

        cartItemRepository.deleteById(cartItemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = findCartByUserId(userId);
        cartItemRepository.deleteAllByCartId(cart.getCartId());
    }

    @Override
    public List<CartItemResponse> getCartItems(Long userId) {
        List<CartItem> cartItems = cartItemRepository.getUserCartItems(userId);
        if (cartItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .toList();

        List<Product> products = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        return cartItems.stream()
                .map(cartItem -> {
                    Product product = productMap.get(cartItem.getProductId());
                    if (product == null) {
                        throw new ResourceNotFoundException(
                                "Product not found for ID: " + cartItem.getProductId()
                        );
                    }
                    return CartItemResponse.fromCartItemAndProduct(cartItem, product);
                })
                .toList();
    }

    private Cart findCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user with id " + userId
                ));
    }

    private Product findProductByIdWithLock(Long productId) {
        return productRepository.findByIdWithPessimisticLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product with id " + productId + " is not found"
                ));
    }

    private void validateAvailableStock(Product product, int requestedQuantity) {
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InventoryException("Insufficient inventory for product " + product.getProductId());
        }
    }
}
