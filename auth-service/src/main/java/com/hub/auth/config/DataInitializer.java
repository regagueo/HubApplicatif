package com.hub.auth.config;

import com.hub.auth.entity.Role;
import com.hub.auth.entity.User;
import com.hub.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfMissing("admin", "admin@portail.intranet", "admin123", "Admin", "Portail", Role.ADMIN);
        createUserIfMissing("emp1", "emp1@portail.intranet", "emp123", "Employé", "Test", Role.EMPLOYEE);
        createUserIfMissing("man1", "man1@portail.intranet", "man123", "Manager", "Test", Role.MANAGER);
        createUserIfMissing("rh1", "rh1@portail.intranet", "rh123", "RH", "Test", Role.RH);
        createUserIfMissing("dg1", "dg1@portail.intranet", "dg123", "Directeur", "Général", Role.DG);
    }

    private void createUserIfMissing(String username, String email, String password, String firstName, String lastName, Role role) {
        userRepository.findByUsername(username).ifPresentOrElse(
                u -> {
                    u.setMfaEnabled(false);
                    u.setMfaSecret(null);
                    u.setEmail(email);
                    u.setFirstName(firstName);
                    u.setLastName(lastName);
                    u.setRoles(Set.of(role));
                    userRepository.save(u);
                },
                () -> userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .firstName(firstName)
                        .lastName(lastName)
                        .roles(Set.of(role))
                        .enabled(true)
                        .mfaEnabled(false)
                        .build())
        );
    }
}
