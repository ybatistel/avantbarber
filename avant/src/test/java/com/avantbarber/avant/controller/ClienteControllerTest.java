package com.avantbarber.avant.controller;

import com.avantbarber.avant.dto.ClienteDTO;
import com.avantbarber.avant.exception.BusinessException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.infra.RestExceptionHandler;
import com.avantbarber.avant.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de camada web isolado (sem contexto Spring) — ver BarbeiroControllerTest para o
 * motivo de não usar {@code @WebMvcTest} neste ambiente. Usa o {@link RestExceptionHandler}
 * real para validar o mapeamento das exceções de domínio em status HTTP.
 */
@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    private MockMvc mockMvc;

    private static final String NUMERO = "5511999999999";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClienteController(clienteService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void buscarPorNumero_deveRetornar200ComOCliente_quandoExiste() throws Exception {
        given(clienteService.buscarPorNumero(NUMERO))
                .willReturn(new ClienteDTO(1L, "Cliente Teste", NUMERO, null, null));

        mockMvc.perform(get("/clientes/busca").param("numero", NUMERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numero").value(NUMERO));
    }

    @Test
    void buscarPorNumero_deveRepassarOValorBrutoAoService() throws Exception {
        given(clienteService.buscarPorNumero("+55 (11) 99999-9999"))
                .willReturn(new ClienteDTO(1L, "Cliente Teste", NUMERO, null, null));

        mockMvc.perform(get("/clientes/busca").param("numero", "+55 (11) 99999-9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(NUMERO));
    }

    @Test
    void buscarPorNumero_deveRetornar404_quandoNaoExiste() throws Exception {
        given(clienteService.buscarPorNumero(NUMERO))
                .willThrow(new RecursoNaoEncontradoException("Cliente não encontrado com o número: " + NUMERO));

        mockMvc.perform(get("/clientes/busca").param("numero", NUMERO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void buscarPorNumero_deveRetornar400_quandoNumeroInvalido() throws Exception {
        given(clienteService.buscarPorNumero("abc"))
                .willThrow(new BusinessException("Erro: O número informado é inválido!"));

        mockMvc.perform(get("/clientes/busca").param("numero", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscarPorNumero_deveRetornar400_quandoParametroNumeroAusente() throws Exception {
        mockMvc.perform(get("/clientes/busca"))
                .andExpect(status().isBadRequest());
    }
}
