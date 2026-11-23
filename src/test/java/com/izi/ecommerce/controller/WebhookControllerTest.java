package com.izi.ecommerce.controller;

import com.izi.ecommerce.model.PaymentNotification;
import com.izi.ecommerce.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WebhookControllerTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentNotification notification = new PaymentNotification();

    @Test
    void acceptsWebhookWithValidToken() {
        WebhookController controller = new WebhookController(paymentService, "secret-token");

        ResponseEntity<String> response = controller.handleXenditWebhook("secret-token", notification);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(paymentService).handleNotification(notification);
    }

    @Test
    void rejectsWebhookWithWrongToken() {
        WebhookController controller = new WebhookController(paymentService, "secret-token");

        ResponseEntity<String> response = controller.handleXenditWebhook("wrong-token", notification);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentService, never()).handleNotification(any());
    }

    @Test
    void rejectsWebhookWithoutToken() {
        WebhookController controller = new WebhookController(paymentService, "secret-token");

        ResponseEntity<String> response = controller.handleXenditWebhook(null, notification);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentService, never()).handleNotification(any());
    }

    @Test
    void rejectsAllWebhooksWhenTokenIsNotConfigured() {
        WebhookController controller = new WebhookController(paymentService, "");

        ResponseEntity<String> response = controller.handleXenditWebhook("", notification);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(paymentService, never()).handleNotification(any());
    }
}
