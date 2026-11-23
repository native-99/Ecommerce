package com.izi.ecommerce.service;

import com.izi.ecommerce.model.UserInfo;

import java.time.Instant;

public interface JwtService {

    String generateToken(UserInfo userInfo);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    Instant getIssuedAtFromToken(String token);

    Instant getExpirationFromToken(String token);
}
