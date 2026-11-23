package com.izi.ecommerce.service;

import com.izi.ecommerce.common.errors.ForbiddenAccessException;
import com.izi.ecommerce.common.errors.ResourceNotFoundException;
import com.izi.ecommerce.entity.UserAddress;
import com.izi.ecommerce.model.UserAddressRequest;
import com.izi.ecommerce.model.UserAddressResponse;
import com.izi.ecommerce.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository userAddressRepository;

    @Override
    @Transactional
    public UserAddressResponse create(Long userId, UserAddressRequest request) {

        if (request.isDefault()) {
            userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(address -> {
                        address.setIsDefault(false);
                        userAddressRepository.save(address);
                    });
        }

        UserAddress newAddress = UserAddress.builder()
                .userId(userId)
                .addressName(request.getAddressName())
                .streetAddress(request.getStreetAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .isDefault(request.isDefault())
                .build();

        UserAddress savedAddress = userAddressRepository.save(newAddress);

        return UserAddressResponse.fromUserAddress(savedAddress);
    }

    @Override
    public List<UserAddressResponse> findByUserId(Long userId) {
        return userAddressRepository.findByUserId(userId)
                .stream()
                .map(UserAddressResponse::fromUserAddress)
                .toList();
    }

    @Override
    public UserAddressResponse findById(Long userId, Long addressId) {
        UserAddress userAddress = findOwnedAddress(userId, addressId);

        return UserAddressResponse.fromUserAddress(userAddress);
    }

    @Override
    @Transactional
    public UserAddressResponse update(Long userId, Long addressId, UserAddressRequest request) {

        UserAddress existingAddress = findOwnedAddress(userId, addressId);

        if (request.isDefault() && !existingAddress.getIsDefault()) {
            userAddressRepository.findByUserIdAndIsDefaultTrue(existingAddress.getUserId())
                    .ifPresent(address -> {
                        address.setIsDefault(false);
                        userAddressRepository.save(address);
                    });
        }

        existingAddress.setAddressName(request.getAddressName());
        existingAddress.setStreetAddress(request.getStreetAddress());
        existingAddress.setCity(request.getCity());
        existingAddress.setState(request.getState());
        existingAddress.setPostalCode(request.getPostalCode());
        existingAddress.setCountry(request.getCountry());
        existingAddress.setIsDefault(request.isDefault());

        UserAddress savedAddress = userAddressRepository.save(existingAddress);

        return UserAddressResponse.fromUserAddress(savedAddress);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long addressId) {

        UserAddress existingAddress = findOwnedAddress(userId, addressId);

        userAddressRepository.delete(existingAddress);

        if (existingAddress.getIsDefault()) {
            List<UserAddress> remainingAddresses =
                    userAddressRepository.findByUserId(existingAddress.getUserId());

            if (!remainingAddresses.isEmpty()) {
                UserAddress newDefaultAddress = remainingAddresses.getFirst();
                newDefaultAddress.setIsDefault(true);
                userAddressRepository.save(newDefaultAddress);
            }
        }
    }

    @Override
    @Transactional
    public UserAddressResponse setDefaultAddress(Long userId, Long addressId) {

        UserAddress existingAddress = findOwnedAddress(userId, addressId);

        userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(address -> {
                    address.setIsDefault(false);
                    userAddressRepository.save(address);
                });

        existingAddress.setIsDefault(true);

        UserAddress savedAddress = userAddressRepository.save(existingAddress);

        return UserAddressResponse.fromUserAddress(savedAddress);
    }

    private UserAddress findOwnedAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address with id " + addressId + " is not found"
                ));

        if (!address.getUserId().equals(userId)) {
            throw new ForbiddenAccessException("Address does not belong to this user");
        }

        return address;
    }
}
