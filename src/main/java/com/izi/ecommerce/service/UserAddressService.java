package com.izi.ecommerce.service;

import com.izi.ecommerce.model.UserAddressRequest;
import com.izi.ecommerce.model.UserAddressResponse;

import java.util.List;

public interface UserAddressService {

    UserAddressResponse create(Long userId, UserAddressRequest request);

    List<UserAddressResponse> findByUserId(Long userId);

    UserAddressResponse findById(Long userId, Long addressId);

    UserAddressResponse update(Long userId, Long addressId, UserAddressRequest request);

    void delete(Long userId, Long addressId);

    UserAddressResponse setDefaultAddress(Long userId, Long addressId);
}
