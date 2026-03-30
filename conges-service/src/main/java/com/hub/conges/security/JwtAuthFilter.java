package com.hub.conges.security;

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
import java.util.ArrayList;
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
        try {
            String bearer = request.getHeader("Authorization");
            String jwt = (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
            if (StringUtils.hasText(jwt) && jwtValidator.validateToken(jwt)) {
                var claims = jwtValidator.getClaims(jwt);
                Long userId = parseUserId(claims.get("userId"));
                if (userId == null) {
                    userId = parseUserId(claims.get("id"));
                }
                if (userId != null) {
                    String username = claims.getSubject();
                    var authorities = parseAuthorities(claims.get("roles"));
                    var principal = new CongesUserPrincipal(userId, username, authorities);
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
                }
            }
        } catch (Exception ignored) {}
        filterChain.doFilter(request, response);
    }

    private static Long parseUserId(Object userIdObj) {
        if (userIdObj instanceof Number n) {
            return n.longValue();
        }
        if (userIdObj instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Collection<SimpleGrantedAuthority> parseAuthorities(Object rolesObj) {
        if (rolesObj == null) {
            return Collections.emptyList();
        }
        var roles = new ArrayList<String>();
        if (rolesObj instanceof String rolesStr) {
            roles.addAll(Stream.of(rolesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList());
        } else if (rolesObj instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (value != null && !String.valueOf(value).isBlank()) {
                    roles.add(String.valueOf(value).trim());
                }
            }
        } else {
            String raw = String.valueOf(rolesObj).trim();
            if (!raw.isBlank()) {
                roles.add(raw);
            }
        }
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(role -> role.toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
