package com.avantbarber.avant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Falha fechada: com {@code n8n.api-key} vazia (o default de {@code application.yaml} quando
 * {@code N8N_API_KEY} não está definida) o acesso do n8n fica desligado — nenhum valor de
 * header, inclusive vazio, autentica.
 */
@SpringJUnitWebConfig(N8nSecurityTestConfig.class)
@TestPropertySource(properties = "n8n.api-key=")
class N8nSecurityDesligadaTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private int statusDe(MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao).andReturn().getResponse().getStatus();
    }

    @Test
    void headerVazio_naoDeveAutenticar_quandoAKeyConfiguradaEVazia() throws Exception {
        assertThat(statusDe(get("/clientes/busca").param("numero", "1").header("X-API-Key", ""))).isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"qualquer-valor", " ", "null", "chave-de-teste"})
    void headerComQualquerValor_naoDeveAutenticar_quandoAKeyConfiguradaEVazia(String valor) throws Exception {
        assertThat(statusDe(get("/clientes/busca").param("numero", "1").header("X-API-Key", valor))).isEqualTo(401);
        assertThat(statusDe(put("/agendamentos/{id}/cancelar", 999999).header("X-API-Key", valor))).isEqualTo(401);
    }

    @Test
    void endpointsPublicos_continuamLiberados_mesmoComOAcessoDoN8nDesligado() throws Exception {
        assertThat(statusDe(get("/barbeiros/publico").header("X-API-Key", "qualquer-valor"))).isEqualTo(200);
    }
}
