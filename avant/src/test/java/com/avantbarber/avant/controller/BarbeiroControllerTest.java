package com.avantbarber.avant.controller;

import com.avantbarber.avant.dto.BarbeiroPublicoDTO;
import com.avantbarber.avant.service.BarbeiroService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de camada web isolado (sem contexto Spring): valida só o contrato HTTP do
 * controller. Não usa {@code @WebMvcTest} porque, neste ambiente, subir o contexto
 * dispara a auto-configuração do cliente OAuth2, que falha ao instanciar o
 * OAuth2AuthorizedClientManager (java.io.IOException: Unable to establish loopback
 * connection) — um problema de infraestrutura pré-existente, não relacionado a este
 * endpoint.
 */
@ExtendWith(MockitoExtension.class)
class BarbeiroControllerTest {

    @Mock
    private BarbeiroService barbeiroService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BarbeiroController(barbeiroService)).build();
    }

    @Test
    void listarBarbeirosPublicoRetornaApenasIdENome() throws Exception {
        given(barbeiroService.listarBarbeirosPublico())
                .willReturn(List.of(new BarbeiroPublicoDTO(1L, "João Barbeiro")));

        mockMvc.perform(get("/barbeiros/publico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("João Barbeiro"))
                .andExpect(jsonPath("$[0].cpf").doesNotExist())
                .andExpect(jsonPath("$[0].numero").doesNotExist())
                .andExpect(jsonPath("$[0].perfil").doesNotExist());
    }

    @Test
    void listarBarbeirosPublicoNuncaSerializaCpfOuNumero() throws Exception {
        given(barbeiroService.listarBarbeirosPublico())
                .willReturn(List.of(new BarbeiroPublicoDTO(2L, "Maria Barbeira")));

        String corpo = mockMvc.perform(get("/barbeiros/publico"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(corpo)
                .doesNotContain("cpf")
                .doesNotContain("numero")
                .doesNotContain("perfil");
    }
}
