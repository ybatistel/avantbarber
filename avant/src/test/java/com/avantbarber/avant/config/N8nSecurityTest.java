package com.avantbarber.avant.config;

import com.avantbarber.avant.dto.AgendamentoRequestDTO;
import com.avantbarber.avant.model.OrigemAgendamento;
import com.avantbarber.avant.service.AgendamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Valida a chain de segurança do n8n (header X-API-Key) com a {@link SpringConfig} real —
 * ver {@link N8nSecurityTestConfig}. Os services são mocks, então nada é persistido.
 */
@SpringJUnitWebConfig(N8nSecurityTestConfig.class)
@TestPropertySource(properties = "n8n.api-key=chave-de-teste")
class N8nSecurityTest {

    private static final String CHAVE_CERTA = "chave-de-teste";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AgendamentoService agendamentoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.reset(agendamentoService);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private static String corpoDoAgendamento(String origem) {
        String campoOrigem = origem == null ? "" : ", \"origem\": \"" + origem + "\"";
        return "{\"clienteId\": 1, \"servicoId\": 1, \"barbeiroId\": 1, \"dataHora\": \"2030-01-07T10:00:00\""
                + campoOrigem + "}";
    }

    private OrigemAgendamento origemRecebidaPeloService() {
        ArgumentCaptor<AgendamentoRequestDTO> captor = ArgumentCaptor.forClass(AgendamentoRequestDTO.class);
        Mockito.verify(agendamentoService).salvar(captor.capture());
        return captor.getValue().getOrigem();
    }

    private int statusDe(MockHttpServletRequestBuilder requisicao) throws Exception {
        return mockMvc.perform(requisicao).andReturn().getResponse().getStatus();
    }

    // Endpoints liberados para o n8n. Os mocks dos services não gravam nada, então o status
    // esperado é "qualquer coisa que não seja 401/403" — a segurança deixou passar.
    static Stream<Arguments> endpointsDaAllowList() {
        return Stream.of(
                Arguments.of("GET /agendamentos/disponiveis",
                        get("/agendamentos/disponiveis").param("barbeiroId", "1").param("data", "2030-01-07").param("servicoId", "1")),
                Arguments.of("GET /clientes/busca", get("/clientes/busca").param("numero", "5511999999999")),
                Arguments.of("POST /clientes",
                        post("/clientes").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of("POST /agendamentos",
                        post("/agendamentos").contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of("PUT /agendamentos/{id}/cancelar", put("/agendamentos/{id}/cancelar", 999999)),
                Arguments.of("PUT /agendamentos/{id}/reagendar",
                        put("/agendamentos/{id}/reagendar", 999999).param("novaData", "2030-01-07T10:00:00")));
    }

    // Tudo que o n8n NÃO pode fazer (negócio): confirmar, apagar, ler dados pessoais em massa.
    static Stream<Arguments> endpointsForaDaAllowList() {
        return Stream.of(
                Arguments.of("GET /clientes", get("/clientes")),
                Arguments.of("GET /clientes/{id}", get("/clientes/{id}", 1)),
                Arguments.of("PUT /clientes/{id}", put("/clientes/{id}", 1).contentType(MediaType.APPLICATION_JSON).content("{}")),
                Arguments.of("DELETE /clientes/{id}", delete("/clientes/{id}", 1)),
                Arguments.of("GET /agendamentos", get("/agendamentos")),
                Arguments.of("GET /agendamentos/{id}", get("/agendamentos/{id}", 1)),
                Arguments.of("PUT /agendamentos/{id}/confirmar", put("/agendamentos/{id}/confirmar", 999999)),
                Arguments.of("GET /barbeiros", get("/barbeiros")),
                Arguments.of("POST /servicos-desejados",
                        post("/servicos-desejados").contentType(MediaType.APPLICATION_JSON).content("{}")));
    }

    // ---------- key inválida ----------

    @Test
    void keyErrada_deveRetornar401_emEndpointDaAllowList() throws Exception {
        assertThat(statusDe(get("/clientes/busca").param("numero", "1").header("X-API-Key", "errada"))).isEqualTo(401);
    }

    @Test
    void keyVazia_deveRetornar401_emEndpointDaAllowList() throws Exception {
        assertThat(statusDe(get("/clientes/busca").param("numero", "1").header("X-API-Key", ""))).isEqualTo(401);
    }

    @Test
    void keyErrada_deveRetornar401_emEndpointForaDaAllowList() throws Exception {
        assertThat(statusDe(get("/clientes").header("X-API-Key", "errada"))).isEqualTo(401);
    }

    @Test
    void keyComMesmoPrefixoDaCerta_deveRetornar401() throws Exception {
        assertThat(statusDe(get("/clientes/busca").param("numero", "1").header("X-API-Key", CHAVE_CERTA + "x"))).isEqualTo(401);
        assertThat(statusDe(get("/clientes/busca").param("numero", "1").header("X-API-Key", "chave-de-test"))).isEqualTo(401);
    }

    // ---------- key correta ----------

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointsDaAllowList")
    void keyCerta_deveDeixarPassarDaSeguranca_emEndpointDaAllowList(String descricao, MockHttpServletRequestBuilder requisicao) throws Exception {
        int status = statusDe(requisicao.header("X-API-Key", CHAVE_CERTA));

        assertThat(status).as(descricao).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpointsForaDaAllowList")
    void keyCerta_deveRetornar403_emEndpointForaDaAllowList(String descricao, MockHttpServletRequestBuilder requisicao) throws Exception {
        int status = statusDe(requisicao.header("X-API-Key", CHAVE_CERTA));

        assertThat(status).as(descricao).isEqualTo(403);
    }

    @Test
    void keyCerta_naoDeveExigirCsrfEmRequisicoesDeEscrita() throws Exception {
        // PUT/POST sem token CSRF: na chain do Google isso dá 403; na do n8n (stateless) passa.
        assertThat(statusDe(put("/agendamentos/{id}/cancelar", 999999).header("X-API-Key", CHAVE_CERTA))).isNotIn(401, 403);
    }

    // ---------- origem derivada da credencial (ponta a ponta, pela chain real) ----------

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"MANUAL", "AUTOMACAO"})
    void keyCerta_deveGravarOrigemAutomacao_independenteDoBody(String origemNoBody) throws Exception {
        int status = statusDe(post("/agendamentos")
                .header("X-API-Key", CHAVE_CERTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDoAgendamento(origemNoBody)));

        assertThat(status).isEqualTo(201);
        assertThat(origemRecebidaPeloService()).isEqualTo(OrigemAgendamento.AUTOMACAO);
    }

    @Test
    void barbeiroLogado_deveManterAOrigemDoBody_semInterferenciaDaRegraDoN8n() throws Exception {
        int status = statusDe(post("/agendamentos")
                .with(user("barbeiro").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDoAgendamento("MANUAL")));

        assertThat(status).isEqualTo(201);
        assertThat(origemRecebidaPeloService()).isEqualTo(OrigemAgendamento.MANUAL);
    }

    @Test
    void barbeiroLogado_deixaAOrigemNulaQuandoOmitida_paraOServiceAplicarODefaultManual() throws Exception {
        int status = statusDe(post("/agendamentos")
                .with(user("barbeiro").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpoDoAgendamento(null)));

        assertThat(status).isEqualTo(201);
        assertThat(origemRecebidaPeloService()).isNull();
    }

    // ---------- endpoints públicos ----------

    @ParameterizedTest
    @MethodSource("endpointsPublicos")
    void endpointsPublicos_deveRetornar200_mesmoComKeyErrada(String caminho) throws Exception {
        assertThat(statusDe(get(caminho).header("X-API-Key", "errada"))).isEqualTo(200);
    }

    @ParameterizedTest
    @MethodSource("endpointsPublicos")
    void endpointsPublicos_deveRetornar200_comKeyCerta(String caminho) throws Exception {
        assertThat(statusDe(get(caminho).header("X-API-Key", CHAVE_CERTA))).isEqualTo(200);
    }

    static Stream<String> endpointsPublicos() {
        return Stream.of("/barbeiros/publico", "/servicos-desejados/publico");
    }

    // ---------- sem header: chain do Google (inalterada) ----------

    @Test
    void semHeader_deveCairNaChainDoGoogleERedirecionarParaLogin() throws Exception {
        assertThat(statusDe(get("/clientes/busca").param("numero", "1"))).isEqualTo(302);
    }

    @Test
    void semHeader_endpointsPublicosContinuamLiberados() throws Exception {
        assertThat(statusDe(get("/barbeiros/publico"))).isEqualTo(200);
        assertThat(statusDe(get("/servicos-desejados/publico"))).isEqualTo(200);
    }

    @Test
    void semHeader_escritaSemCsrfContinuaBloqueadaNaChainDoGoogle() throws Exception {
        assertThat(statusDe(put("/agendamentos/{id}/cancelar", 999999))).isEqualTo(403);
    }
}
