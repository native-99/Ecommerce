package com.izi.ecommerce.service;

public interface TokenRevocationService {

    void revoke(String token);

    boolean isRevoked(String token);
}
