package com.izi.ecommerce.config;

import com.izi.ecommerce.config.middleware.JwtAuthenticationFilter;
import com.izi.ecommerce.model.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ApiSecurityConfiguration {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    // Comma separated, e.g. https://shop.example.com,https://admin.example.com
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers(
                            "/auth/**",
                            "/hello",
                            "/api-docs/**",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/webhook/xendit"
                    ).permitAll();
                    registry.requestMatchers(HttpMethod.GET, "/products/**").permitAll();
                    registry.requestMatchers(HttpMethod.POST, "/products/**").hasAuthority("ROLE_ADMIN");
                    registry.requestMatchers(HttpMethod.PUT, "/products/**").hasAuthority("ROLE_ADMIN");
                    registry.requestMatchers(HttpMethod.DELETE, "/products/**").hasAuthority("ROLE_ADMIN");
                    registry.requestMatchers("/admin/**")
                            .hasAuthority("ROLE_ADMIN");

                    registry.requestMatchers(
                            HttpMethod.PUT,
                            "/orders/*/status"
                    ).hasAuthority("ROLE_ADMIN");

                    registry.requestMatchers(
                            HttpMethod.POST,
                            "/shippings/order"
                    ).hasAuthority("ROLE_ADMIN");

                    registry.anyRequest().authenticated();
                })
                .sessionManagement(configurer -> {
                    configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .exceptionHandling(configurer -> configurer
                        .authenticationEntryPoint((request, response, exception) ->
                                writeErrorResponse(response, HttpStatus.UNAUTHORIZED, exception.getMessage())
                        )
                        .accessDeniedHandler((request, response, exception) -> {
                            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            HttpStatus status = isAnonymous(authentication)
                                    ? HttpStatus.UNAUTHORIZED
                                    : HttpStatus.FORBIDDEN;

                            writeErrorResponse(response, status, exception.getMessage());
                        })
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    private boolean isAnonymous(Authentication authentication) {
        return authentication == null ||
                authentication instanceof AnonymousAuthenticationToken ||
                !authentication.isAuthenticated();
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
