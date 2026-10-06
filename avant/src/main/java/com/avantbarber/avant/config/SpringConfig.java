package com.avantbarber.avant.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SpringConfig {

    private static final String[] ENDPOINTS_PUBLICOS = {
            "/", "/login", "/barbeiros/publico", "/servicos-desejados/publico"
    };

    private static final String[] PUBLICOS_GET = {"/barbeiros/publico", "/servicos-desejados/publico"};

    /**
     * Chain exclusiva do n8n: só captura requisições com o header X-API-Key, é stateless e
     * libera apenas as operações que o n8n pode executar — todo o resto é negado, então a
     * key nunca alcança confirmar agendamento, DELETE, nem a leitura de dados pessoais.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain n8nSecurityFilterChain(HttpSecurity http,
            @Value("${n8n.api-key:}") String apiKey) {
        return http.securityMatcher(request -> request.getHeader(ApiKeyAuthFilter.HEADER) != null)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new ApiKeyAuthFilter(apiKey), AuthorizationFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers(HttpMethod.GET, PUBLICOS_GET).permitAll();
                    registry.requestMatchers(HttpMethod.GET, "/agendamentos/disponiveis", "/clientes/busca")
                            .hasRole(ApiKeyAuthFilter.ROLE_N8N);
                    registry.requestMatchers(HttpMethod.POST, "/clientes", "/agendamentos")
                            .hasRole(ApiKeyAuthFilter.ROLE_N8N);
                    registry.requestMatchers(HttpMethod.PUT, "/agendamentos/{id}/cancelar", "/agendamentos/{id}/reagendar")
                            .hasRole(ApiKeyAuthFilter.ROLE_N8N);
                    registry.anyRequest().denyAll();
                })
                .build();
    }

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
