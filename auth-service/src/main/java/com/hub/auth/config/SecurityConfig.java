package com.hub.auth.config;

import com.hub.auth.security.JwtAuthenticationFilter;
import com.hub.auth.security.AzureRoleMappingOidcUserService;
import com.hub.auth.security.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final AzureRoleMappingOidcUserService azureRoleMappingOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Value("${app.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Public routes for OAuth2 flow and health landing
                        .requestMatchers("/", "/error", "/login", "/oauth2/**", "/login/oauth2/**")
                        .permitAll()
                        .requestMatchers("/auth/register", "/auth/login", "/auth/mfa/verify", "/auth/mfa/resend-otp")
                        .permitAll()
                        .requestMatchers("/actuator/**", "/actuator/prometheus").permitAll()
                        .requestMatchers("/home", "/me").authenticated()
                        .requestMatchers("/api/rh/**").hasRole("RH")
                        // placeholders (non activés pour le moment)
                        // .requestMatchers("/api/employee/**").hasRole("EMPLOYEE")
                        // .requestMatchers("/api/manager/**").hasRole("MANAGER")
                        // .requestMatchers("/api/dg/**").hasRole("DG")
                        // .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/auth/users/peers").authenticated()
                        .requestMatchers("/auth/users/**").hasAnyRole("ADMIN", "MANAGER", "RH")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(oauth2Enabled ? SessionCreationPolicy.IF_REQUIRED : SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter()
                                    .write("{\"error\":\"Session expirée ou token invalide. Reconnectez-vous.\"}");
                        }))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (oauth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    // Fallback: si jamais le success handler n'est pas utilisé,
                    // on redirige vers /home (endpoint backend).
                    .defaultSuccessUrl("/home", true)
                    .successHandler(oauth2AuthenticationSuccessHandler)
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(azureRoleMappingOidcUserService)));
        }

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
