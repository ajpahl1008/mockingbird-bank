package com.mockingbirdbank.security;

import com.mockingbirdbank.ui.view.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * {@code VaadinWebSecurity} was removed in Vaadin 25 in favor of this
 * composable {@code VaadinSecurityConfigurer} style - see
 * https://vaadin.com/docs/latest/flow/security/enabling-security.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Actuator health has to stay reachable without a session - the k8s
        // readiness probe and CI smoke checks both hit it unauthenticated.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll());

        http.with(VaadinSecurityConfigurer.vaadin(), configurer ->
                configurer.loginView(LoginView.class));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
