package com.izi.ecommerce.config.middleware;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.izi.ecommerce.model.UserInfo;
import com.izi.ecommerce.service.JwtService;
import com.izi.ecommerce.service.TokenRevocationService;
import com.izi.ecommerce.service.UserDetailsImpl;
import org.springframework.web.servlet.HandlerExceptionResolver;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
@Component
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserDetailsImpl userDetailsService;
    private final JwtService jwtService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final TokenRevocationService tokenRevocationService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String jwt = authHeader.substring(7);

        try {
            if (jwtService.validateToken(jwt) && !tokenRevocationService.isRevoked(jwt)) {
                final String userIdentifier = jwtService.getUsernameFromToken(jwt);
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (userIdentifier != null && authentication == null) {
                    UserInfo userInfo = (UserInfo) userDetailsService.loadUserByUsername(userIdentifier);

                    if (isTokenStillValidFor(userInfo, jwt)) {
                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(
                                        userInfo,
                                        null,
                                        userInfo.getAuthorities()
                                );
                        authenticationToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }

    // A signed token is not enough: the account must still be enabled and the token
    // must have been issued after the last password change.
    private boolean isTokenStillValidFor(UserInfo userInfo, String jwt) {
        if (!userInfo.isEnabled()) {
            return false;
        }

        LocalDateTime passwordChangedAt = userInfo.getUser().getPasswordChangedAt();
        if (passwordChangedAt == null) {
            return true;
        }

        // JWT "iat" only has second precision, so compare at that precision.
        Instant changedAt = passwordChangedAt.atZone(ZoneId.systemDefault())
                .toInstant()
                .truncatedTo(ChronoUnit.SECONDS);
        Instant issuedAt = jwtService.getIssuedAtFromToken(jwt);

        return !issuedAt.isBefore(changedAt);
    }
}
