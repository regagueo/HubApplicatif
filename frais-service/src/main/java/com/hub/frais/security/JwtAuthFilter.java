package com.hub.frais.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    public JwtAuthFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String bearer = request.getHeader("Authorization");
        String jwt = (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
        if (StringUtils.hasText(jwt) && jwtValidator.validateToken(jwt)) {
            var claims = jwtValidator.getClaims(jwt);

            Object userIdClaim = claims.get("userId");
            if (userIdClaim == null) userIdClaim = claims.get("id");
            Long userId = null;
            if (userIdClaim instanceof Number) {
                userId = ((Number) userIdClaim).longValue();
            } else if (userIdClaim instanceof String && StringUtils.hasText((String) userIdClaim)) {
                userId = Long.parseLong((String) userIdClaim);
            }

            String username = claims.getSubject();
            Object rolesClaim = claims.get("roles");

            var authorities = Collections.<SimpleGrantedAuthority>emptyList();
            if (rolesClaim instanceof String rolesStr && StringUtils.hasText(rolesStr)) {
                authorities = Stream.of(rolesStr.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .map(r -> r.toUpperCase(Locale.ROOT))
                        .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toList());
            } else if (rolesClaim instanceof Collection<?> rolesCol) {
                authorities = rolesCol.stream()
                        .map(String::valueOf)
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .map(r -> r.toUpperCase(Locale.ROOT))
                        .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toList());
            }

            var principal = new FraisUserPrincipal(userId, username, authorities);
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities)
            );
        }
        filterChain.doFilter(request, response);
    }
}
