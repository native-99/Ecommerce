package com.izi.ecommerce.service;

import com.izi.ecommerce.common.errors.InventoryException;
import com.izi.ecommerce.entity.Product;
import com.izi.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final CacheService cacheService;

    @Override
    @Transactional
    public boolean checkAndLockInventory(Map<Long, Integer> productQuantities) {
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findByIdWithPessimisticLock(entry.getKey()).orElseThrow(
                    () -> new InventoryException("Product not found with id " + entry.getKey())
            );
            if(product.getStockQuantity() < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void decreaseQuantity(Map<Long, Integer> productQuantities) {
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findByIdWithPessimisticLock(entry.getKey()).orElseThrow(
                    () -> new InventoryException("Product not found with id " + entry.getKey())
            );

            if (product.getStockQuantity() < entry.getValue()) {
                throw new InventoryException("Insufficient inventory for product " + entry.getKey());
            }

            Integer newQuantity = product.getStockQuantity() - entry.getValue();
            product.setStockQuantity(newQuantity);
            productRepository.save(product);
            evictProductCacheAfterCommit(product.getProductId());
        }
    }

    @Override
    @Transactional
    public void increaseQuantity(Map<Long, Integer> productQuantities) {
        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Product product = productRepository.findByIdWithPessimisticLock(entry.getKey()).orElseThrow(
                    () -> new InventoryException("Product not found with id " + entry.getKey())
            );

            Integer newQuantity = product.getStockQuantity() + entry.getValue();
            product.setStockQuantity(newQuantity);
            productRepository.save(product);
            evictProductCacheAfterCommit(product.getProductId());
        }
    }

    private void evictProductCacheAfterCommit(Long productId) {
        String cacheKey = "product:" + productId;

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cacheService.evict(cacheKey);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cacheService.evict(cacheKey);
                    }
                }
        );
    }
}
