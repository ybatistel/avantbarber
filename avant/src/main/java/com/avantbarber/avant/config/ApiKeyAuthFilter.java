package com.avantbarber.avant.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica o n8n pelo header {@code X-API-Key}. Não é um bean de propósito: assim o Spring
 * Boot não o registra como filtro global do container, só a chain do n8n o utiliza.
 * Falha fechada: com a key configurada vazia, nenhuma requisição é autenticada.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    public static final String ROLE_N8N = "N8N";

    private final byte[] apiKeyEsperada;

    public ApiKeyAuthFilter(String apiKeyEsperada) {
        this.apiKeyEsperada = apiKeyEsperada.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKeyEnviada = request.getHeader(HEADER);
        if (apiKeyEsperada.length > 0 && apiKeyEnviada != null
                && MessageDigest.isEqual(apiKeyEsperada, apiKeyEnviada.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "n8n", null, List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_N8N))));
        }
        filterChain.doFilter(request, response);
    }
}
