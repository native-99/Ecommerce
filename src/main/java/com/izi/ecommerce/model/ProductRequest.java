package com.izi.ecommerce.model;


import com.izi.ecommerce.entity.User;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import tools.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(SnakeCaseStrategy.class)
public class ProductRequest {

    @NotBlank(message = "Nama produk tidak boleh kosong")
    @Size(min = 2, max = 100, message = "Nama produk harus antara 2 dan 100 karakter")
    private String name;

    @NotNull(message = "Harga tidak boleh kosong")
    @Positive(message = "Harga harus lebih besar dari 0")
    @Digits(integer = 10, fraction = 2, message = "Harga harus memiliki maksimal 10 digit dan 2 angka di belakang koma")
    private BigDecimal price;

    @NotNull(message = "Deskripsi produk tidak boleh null")
    @Size(max = 1000, message = "Deskripsi produk tidak boleh lebih dari 1000 karakter")
    private String description;

    @NotNull(message = "Stok produk tidak boleh kosong")
    @Positive(message = "Stok produk harus lebih besar dari 0")
    private Integer stockQuantity;

    @NotNull(message = "Berat produk tidak boleh kosong")
    @Positive(message = "Berat produk harus lebih besar dari 0")
    @Digits(integer = 10, fraction = 2, message = "Berat produk harus memiliki maksimal 10 digit dan 2 angka di belakang koma")
    private BigDecimal weight;

    @NotEmpty(message = "Harus ada satu category yang dipilih")
    private List<Long> categoryIds;


    private User user;
}
