package com.hub.auth.security;

import com.hub.auth.entity.Role;
import com.hub.auth.entity.User;
import com.hub.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.oauth2.frontend-base-url:http://localhost:3001}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OidcUser oidcUser)) {
            response.sendRedirect(frontendBaseUrl + "/login?ssoError=Principal+OIDC+invalide");
            return;
        }

        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            email = oidcUser.getPreferredUsername();
        }
        if (email == null || email.isBlank()) {
            response.sendRedirect(frontendBaseUrl + "/login?ssoError=Email+introuvable");
            return;
        }

        String usernameBase = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        if (usernameBase.isBlank()) {
            usernameBase = "azure.user";
        }

        Set<Role> roles = new HashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_RH".equals(authority.getAuthority())) {
                roles.add(Role.RH);
            }
        }
        if (roles.isEmpty()) {
            roles.add(Role.EMPLOYEE);
        }

        final String resolvedEmail = email;
        final String resolvedUsernameBase = usernameBase;
        User user = userRepository.findByEmail(resolvedEmail).orElseGet(() -> {
            String username = buildUniqueUsername(resolvedUsernameBase);
            return User.builder()
                    .username(username)
                    .email(resolvedEmail)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .firstName(oidcUser.getGivenName())
                    .lastName(oidcUser.getFamilyName())
                    .enabled(true)
                    .roles(roles)
                    .mfaEnabled(false)
                    .build();
        });

        user.setFirstName(oidcUser.getGivenName() != null ? oidcUser.getGivenName() : user.getFirstName());
        user.setLastName(oidcUser.getFamilyName() != null ? oidcUser.getFamilyName() : user.getLastName());
        user.setRoles(roles);
        user = userRepository.save(user);

        String appJwt = jwtService.generateAccessToken(user);
        // IMPORTANT: on redirige d'abord vers /login côté frontend.
        // Comme les routes (ex: /rh) sont protégées par ProtectedRoute (token côté React),
        // rediriger directement vers /rh échoue tant que le token n'est pas encore chargé dans le frontend.
        String redirect = UriComponentsBuilder.fromUriString(frontendBaseUrl + "/login")
                .queryParam("ssoToken", appJwt)
                .build(true)
                .toUriString();
        response.sendRedirect(redirect);
    }

    private String buildUniqueUsername(String base) {
        String normalized = base.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (normalized.isBlank()) normalized = "azure.user";
        if (!userRepository.existsByUsername(normalized)) {
            return normalized;
        }
        int i = 1;
        while (userRepository.existsByUsername(normalized + i)) {
            i++;
        }
        return normalized + i;
    }
}

