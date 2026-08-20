package com.avantbarber.avant.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SpringConfig {

    private static final String[] ENDPOINTS_PUBLICOS = {
            "/", "/login", "/barbeiros/publico", "/servicos-desejados/publico"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.cors(cors -> {})
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers(ENDPOINTS_PUBLICOS).permitAll();
                    registry.anyRequest().authenticated();
                }).oauth2Login(oauth2 -> {
                    oauth2.loginPage("/login")
                            .successHandler((request, response, authentication) -> {response.sendRedirect("/profile");});
                })
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        for (String endpoint : ENDPOINTS_PUBLICOS) {
            if (!endpoint.equals("/") && !endpoint.equals("/login")) {
                source.registerCorsConfiguration(endpoint, configuration);
            }
        }
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
