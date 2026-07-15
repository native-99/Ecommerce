package com.izi.ecommerce.controller;

import com.izi.ecommerce.common.PageUtil;
import com.izi.ecommerce.model.PaginatedProductResponse;
import com.izi.ecommerce.model.ProductRequest;
import com.izi.ecommerce.model.ProductResponse;
import com.izi.ecommerce.model.UserInfo;
import com.izi.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // GET localhost:3000/api/v1/products/2
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findProductById(
            @PathVariable(value = "id") Long productId
    ) {
        return ResponseEntity.ok(productService.findById(productId));
    }

    // GET localhost:3000/api/v1/products
    @GetMapping
    public ResponseEntity<PaginatedProductResponse> getAllProduct(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "product_id,asc") String[] sort,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryName
    ) {
        List<Sort.Order> orders = PageUtil.parseProductSortOrderRequest(sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        Page<ProductResponse> productResponses;

        if (name != null && !name.isEmpty()) {
            productResponses = productService.findByNameAndPageable(name, pageable);
        } else if (categoryName != null && !categoryName.isBlank()) {
            productResponses = productService.findByCategoryName(categoryName, pageable);
        } else {
            productResponses = productService.findByPage(pageable);
        }

        return ResponseEntity.ok(productService.convertProductPage(productResponses));
    }

    // POST localhost:3000/api/v1/products
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) authentication.getPrincipal();

        request.setUser(userInfo.getUser());

        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT localhost:3000/api/v1/products/2
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @Valid @RequestBody ProductRequest request,
            @PathVariable(name = "id") Long productId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) authentication.getPrincipal();
        request.setUser(userInfo.getUser());

        return ResponseEntity.ok(productService.update(productId, request));
    }

    // DELETE localhost:3000/api/v1/products/2
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable(name = "id") Long productId
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) authentication.getPrincipal();
        productService.delete(productId, userInfo.getUser().getUserId());
        return ResponseEntity.noContent().build();
    }
}
