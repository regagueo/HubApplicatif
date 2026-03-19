package com.hub.dg.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtValidator jwtValidator;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String bearer = request.getHeader("Authorization");
            String jwt = (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
            if (StringUtils.hasText(jwt) && jwtValidator.validateToken(jwt)) {
                var claims = jwtValidator.getClaims(jwt);
                Long userId = claims.get("userId", Long.class);
                String username = claims.getSubject();
                String rolesStr = (String) claims.get("roles");
                var authorities = rolesStr != null
                        ? Stream.of(rolesStr.split(",")).map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim())).collect(Collectors.toList())
                        : Collections.<SimpleGrantedAuthority>emptyList();
                var principal = new DgUserPrincipal(userId, username, authorities);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
            }
        } catch (Exception ignored) {}
        filterChain.doFilter(request, response);
    }
}
