package com.izi.ecommerce.controller;

import com.izi.ecommerce.model.UserUpdateRequest;
import com.izi.ecommerce.model.UserInfo;
import com.izi.ecommerce.model.UserResponse;
import com.izi.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        UserResponse userResponse = userService.findById(getCurrentUserId());
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserResponse userResponse = userService.updateUser(getCurrentUserId(), request);
        return ResponseEntity.ok(userResponse);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe() {
        userService.deleteUser(getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserInfo userInfo = (UserInfo) authentication.getPrincipal();
        return userInfo.getUser().getUserId();
    }
}
