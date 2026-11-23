package com.izi.ecommerce.config.middleware;

import com.izi.ecommerce.entity.Role;
import com.izi.ecommerce.entity.User;
import com.izi.ecommerce.model.UserInfo;
import com.izi.ecommerce.service.JwtService;
import com.izi.ecommerce.service.TokenRevocationService;
import com.izi.ecommerce.service.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String TOKEN = "token";
    private static final Instant ISSUED_AT = Instant.parse("2026-01-10T10:00:00Z");

    private final UserDetailsImpl userDetailsService = mock(UserDetailsImpl.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final FilterChain filterChain = mock(FilterChain.class);
    private final TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            userDetailsService, jwtService, mock(HandlerExceptionResolver.class), tokenRevocationService);

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private User user;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        response = new MockHttpServletResponse();

        user = User.builder().userId(1L).username("buyer").enabled(true).build();
        Role role = new Role();
        role.setName("ROLE_USER");

        when(jwtService.validateToken(TOKEN)).thenReturn(true);
        when(jwtService.getUsernameFromToken(TOKEN)).thenReturn("buyer");
        when(jwtService.getIssuedAtFromToken(TOKEN)).thenReturn(ISSUED_AT);
        when(userDetailsService.loadUserByUsername("buyer"))
                .thenReturn(UserInfo.builder().user(user).roles(List.of(role)).build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidToken() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignoresTokenOfDisabledUser() throws Exception {
        user.setEnabled(false);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresTokenIssuedBeforePasswordChange() throws Exception {
        user.setPasswordChangedAt(toLocal(ISSUED_AT.plusSeconds(60)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void acceptsTokenIssuedAfterPasswordChange() throws Exception {
        user.setPasswordChangedAt(toLocal(ISSUED_AT.minusSeconds(60)));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void ignoresRevokedToken() throws Exception {
        when(tokenRevocationService.isRevoked(TOKEN)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
