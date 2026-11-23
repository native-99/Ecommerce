package com.izi.ecommerce.controller;

import com.izi.ecommerce.model.PaymentNotification;
import com.izi.ecommerce.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("webhook")
@Slf4j
public class WebhookController {

    static final String CALLBACK_TOKEN_HEADER = "x-callback-token";

    private final PaymentService paymentService;
    private final String callbackToken;

    public WebhookController(
            PaymentService paymentService,
            @Value("${xendit.callback-token:}") String callbackToken
    ) {
        this.paymentService = paymentService;
        this.callbackToken = callbackToken;
    }

    // POST host/api/v1/webhook/xendit
    @PostMapping("/xendit")
    public ResponseEntity<String> handleXenditWebhook(
            @RequestHeader(value = CALLBACK_TOKEN_HEADER, required = false) String requestToken,
            @RequestBody PaymentNotification paymentNotification
    ) {
        if (!isValidCallbackToken(requestToken)) {
            log.warn("Rejected Xendit webhook with invalid callback token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid callback token");
        }

        paymentService.handleNotification(paymentNotification);
        return ResponseEntity.ok("OK");
    }

    private boolean isValidCallbackToken(String requestToken) {
        if (callbackToken == null || callbackToken.isBlank() || requestToken == null) {
            return false;
        }

        // Constant-time comparison so the token cannot be guessed from response timing.
        return MessageDigest.isEqual(
                callbackToken.getBytes(StandardCharsets.UTF_8),
                requestToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
