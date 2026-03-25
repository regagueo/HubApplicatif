package com.hub.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AzureRoleMappingOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    @Value("${app.azure.groups.rh}")
    private String rhGroupObjectId;

    private final OidcUserService delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        Collection<? extends GrantedAuthority> mappedAuthorities = mapAuthorities(oidcUser, userRequest);
        String nameAttributeKey = oidcUser.getClaims().containsKey("preferred_username") ? "preferred_username" : "sub";
        return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), nameAttributeKey);
    }

    Collection<? extends GrantedAuthority> mapAuthorities(OidcUser oidcUser, OidcUserRequest userRequest) {
        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());

        List<String> groups = oidcUser.getClaimAsStringList("groups");
        if (groups == null) {
            groups = new ArrayList<>();
        }
        if (groups.isEmpty()) {
            groups = fetchGroupIdsFromGraph(userRequest.getAccessToken().getTokenValue());
        }

        if (rhGroupObjectId != null && !rhGroupObjectId.isBlank() && groups.contains(rhGroupObjectId)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_RH"));
        }

        // placeholders pour mapping futur (non activé)
        // if (groups.contains("<employee-group-id>")) authorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
        // if (groups.contains("<manager-group-id>")) authorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
        // if (groups.contains("<dg-group-id>")) authorities.add(new SimpleGrantedAuthority("ROLE_DG"));
        // if (groups.contains("<admin-group-id>")) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        return authorities;
    }

    private List<String> fetchGroupIdsFromGraph(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://graph.microsoft.com/v1.0/me/memberOf?$select=id",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !(body.get("value") instanceof List<?> values)) {
                return List.of();
            }

            Set<String> ids = new LinkedHashSet<>();
            for (Object item : values) {
                if (item instanceof Map<?, ?> map) {
                    Object id = map.get("id");
                    if (id != null && !String.valueOf(id).isBlank()) {
                        ids.add(String.valueOf(id));
                    }
                }
            }
            return ids.stream().toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }
}

