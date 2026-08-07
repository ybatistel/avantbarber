package com.avantbarber.avant.controller;

import com.avantbarber.avant.dto.ServicoDesejadoDTO;
import com.avantbarber.avant.service.ServicoDesejadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de camada web isolado (sem contexto Spring) — ver BarbeiroControllerTest para o
 * motivo de não usar {@code @WebMvcTest} neste ambiente.
 */
@ExtendWith(MockitoExtension.class)
class ServicoDesejadoControllerTest {

    @Mock
    private ServicoDesejadoService servicoDesejadoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ServicoDesejadoController(servicoDesejadoService)).build();
    }

    @Test
    void listarServicosDesejadosPublicoRetornaNomeEPreco() throws Exception {
        given(servicoDesejadoService.listarServicosDesejados())
                .willReturn(List.of(new ServicoDesejadoDTO(1L, "Corte", new BigDecimal("50.00"))));

        mockMvc.perform(get("/servicos-desejados/publico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Corte"))
                .andExpect(jsonPath("$[0].preco").value(50.00));
    }
}
