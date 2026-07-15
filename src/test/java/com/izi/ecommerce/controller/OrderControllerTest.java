package com.izi.ecommerce.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.izi.ecommerce.entity.Role;
import com.izi.ecommerce.entity.User;
import com.izi.ecommerce.model.CheckoutRequest;
import com.izi.ecommerce.model.OrderResponse;
import com.izi.ecommerce.model.OrderStatus;
import com.izi.ecommerce.model.UserInfo;
import com.izi.ecommerce.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private CheckoutRequest checkoutRequest;
    private OrderResponse orderResponse;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        checkoutRequest = CheckoutRequest.builder()
                .selectedCartItemIds(Arrays.asList(1L, 2L))
                .userAddressId(1L)
                .build();

        orderResponse = OrderResponse.builder()
                .orderId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .shippingFee(new BigDecimal("10.00"))
                .paymentUrl("http://payment.url")
                .build();

        User user = User.builder()
                .userId(1L)
                .username("user@example.com")
                .email("user@example.com")
                .password("password")
                .enabled(true)
                .build();

        Role role = new Role();
        role.setName("ROLE_USER");

        userInfo = UserInfo.builder()
                .user(user)
                .roles(List.of(role))
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userInfo, null, userInfo.getAuthorities());

        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCheckout_WhenRequestIsValid() throws Exception {
        when(orderService.checkout(any(CheckoutRequest.class))).thenReturn(orderResponse);

        mockMvc.perform(post("/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "selectedCartItemIds", checkoutRequest.getSelectedCartItemIds(),
                                "userAddressId", checkoutRequest.getUserAddressId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(100.00))
                .andExpect(jsonPath("$.shippingFee").value(10.00))
                .andExpect(jsonPath("$.paymentUrl").value("http://payment.url"))
                .andExpect(jsonPath("$.orderId").value(1));

        verify(orderService).checkout(argThat(request ->
                Long.valueOf(1L).equals(request.getUserId())
                        && request.getSelectedCartItemIds().equals(checkoutRequest.getSelectedCartItemIds())
                        && request.getUserAddressId().equals(checkoutRequest.getUserAddressId())
        ));
    }

    @Test
    void testCheckout_WhenRequestIsInvalid() throws Exception {
        CheckoutRequest invalidRequest = new CheckoutRequest();

        mockMvc.perform(post("/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).checkout(any(CheckoutRequest.class));
    }
}
