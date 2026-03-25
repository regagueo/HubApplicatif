package com.hub.auth.controller;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class OAuth2FlowController {

    @GetMapping("/")
    public ResponseEntity<String> root() {
        return ResponseEntity.ok("Auth Service Running");
    }

    @GetMapping("/home")
    public ResponseEntity<Map<String, Object>> home(
            Authentication authentication,
            @RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient authorizedClient
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        Object principal = authentication.getPrincipal();
        String name = authentication.getName();
        String email = null;
        List<String> groups = Collections.emptyList();

        if (principal instanceof OidcUser oidcUser) {
            name = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getName();
            email = oidcUser.getEmail();
            if (email == null || email.isBlank()) {
                email = oidcUser.getPreferredUsername();
            }
            List<String> claimGroups = oidcUser.getClaimAsStringList("groups");
            groups = claimGroups != null ? claimGroups : Collections.emptyList();
        }

        // Fallback Graph API: when claim `groups` is not present in id_token,
        // query Microsoft Graph for effective memberships.
        if ((groups == null || groups.isEmpty()) && authorizedClient != null && authorizedClient.getAccessToken() != null) {
            List<String> graphGroups = fetchAzureGroups(authorizedClient.getAccessToken().getTokenValue());
            if (!graphGroups.isEmpty()) {
                groups = graphGroups;
            }
        }

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .collect(Collectors.toSet());

        return ResponseEntity.ok(Map.of(
                "name", name,
                "email", email != null ? email : "",
                "roles", roles,
                "groups", groups
        ));
    }

    private List<String> fetchAzureGroups(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://graph.microsoft.com/v1.0/me/memberOf?$select=id,displayName",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !(body.get("value") instanceof List<?> values)) {
                return Collections.emptyList();
            }

            Set<String> result = new LinkedHashSet<>();
            for (Object item : values) {
                if (item instanceof Map<?, ?> map) {
                    Object displayName = map.get("displayName");
                    Object id = map.get("id");
                    if (displayName != null && !String.valueOf(displayName).isBlank()) {
                        result.add(String.valueOf(displayName));
                    } else if (id != null && !String.valueOf(id).isBlank()) {
                        result.add(String.valueOf(id));
                    }
                }
            }
            return result.stream().toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}

