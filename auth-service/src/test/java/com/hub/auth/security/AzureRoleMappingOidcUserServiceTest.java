package com.hub.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AzureRoleMappingOidcUserServiceTest {

    @Test
    void shouldMapRhRoleWhenUserBelongsToRhAzureGroup() {
        AzureRoleMappingOidcUserService service = new AzureRoleMappingOidcUserService();
        ReflectionTestUtils.setField(service, "rhGroupObjectId", "4f76fe10-27df-47ce-8442-2f7eab692d2c");

        OidcIdToken idToken = new OidcIdToken(
                "fake-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of(
                        "sub", "user-rh-1",
                        "name", "Fatima RH",
                        "preferred_username", "fatima.rh@intranet.local",
                        "email", "fatima.rh@intranet.local",
                        "groups", List.of("4f76fe10-27df-47ce-8442-2f7eab692d2c")
                )
        );

        OidcUser oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("OIDC_USER")),
                idToken,
                "preferred_username"
        );

        var authorities = service.mapAuthorities(oidcUser);

        assertThat(authorities)
                .extracting(a -> a.getAuthority())
                .contains("ROLE_RH");
    }
}

