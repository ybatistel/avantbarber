package com.avantbarber.avant.controller;

import com.avantbarber.avant.dto.AgendamentoDTO;
import com.avantbarber.avant.model.OrigemAgendamento;
import com.avantbarber.avant.model.StatusAgendamento;
import com.avantbarber.avant.service.AgendamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de camada web isolado (sem contexto Spring) — ver BarbeiroControllerTest para o
 * motivo de não usar {@code @WebMvcTest} neste ambiente.
 */
@ExtendWith(MockitoExtension.class)
class AgendamentoControllerTest {

    @Mock
    private AgendamentoService agendamentoService;

    private MockMvc mockMvc;

    private static final Long AGENDAMENTO_ID = 1L;
    private static final Long BARBEIRO_ID = 1L;
    private static final Long CLIENTE_ID = 1L;
    private static final Long SERVICO_ID = 1L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AgendamentoController(agendamentoService)).build();
    }

    private static AgendamentoDTO agendamentoDTO(LocalDateTime data, StatusAgendamento status) {
        return new AgendamentoDTO(
                AGENDAMENTO_ID, CLIENTE_ID, SERVICO_ID, BARBEIRO_ID,
                "Barbeiro Teste", "Cliente Teste", "Corte", BigDecimal.valueOf(50),
                "12345678900", data, status, OrigemAgendamento.MANUAL
        );
    }

    @Test
    void buscarTodos_deveRetornarListaDeAgendamentos() throws Exception {
        given(agendamentoService.listarAgendamentos())
                .willReturn(List.of(agendamentoDTO(LocalDateTime.now().plusDays(1), StatusAgendamento.PENDENTE)));

        mockMvc.perform(get("/agendamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void buscarPorId_deveRetornarAgendamento_quandoExiste() throws Exception {
        given(agendamentoService.buscarPorId(AGENDAMENTO_ID))
                .willReturn(agendamentoDTO(LocalDateTime.now().plusDays(1), StatusAgendamento.PENDENTE));

        mockMvc.perform(get("/agendamentos/{id}", AGENDAMENTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void listarHorariosDisponiveis_deveRepassarServicoIdAoService() throws Exception {
        given(agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, java.time.LocalDate.of(2027, 1, 5), SERVICO_ID))
                .willReturn(List.of(LocalTime.of(10, 0), LocalTime.of(10, 30)));

        mockMvc.perform(get("/agendamentos/disponiveis")
                        .param("barbeiroId", String.valueOf(BARBEIRO_ID))
                        .param("data", "2027-01-05")
                        .param("servicoId", String.valueOf(SERVICO_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("10:00:00"))
                .andExpect(jsonPath("$[1]").value("10:30:00"));

        verify(agendamentoService).listarHorariosDisponiveis(BARBEIRO_ID, java.time.LocalDate.of(2027, 1, 5), SERVICO_ID);
    }

    @Test
    void listarHorariosDisponiveis_deveRetornar400_quandoServicoIdAusente() throws Exception {
        mockMvc.perform(get("/agendamentos/disponiveis")
                        .param("barbeiroId", String.valueOf(BARBEIRO_ID))
                        .param("data", "2027-01-05"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void salvar_deveRetornar201ComAgendamentoCriado() throws Exception {
        LocalDateTime data = LocalDateTime.of(2027, 1, 5, 10, 0);
        given(agendamentoService.salvar(any()))
                .willReturn(agendamentoDTO(data, StatusAgendamento.PENDENTE));

        String corpo = """
                {
                  "clienteId": 1,
                  "servicoId": 1,
                  "barbeiroId": 1,
                  "dataHora": "2027-01-05T10:00:00"
                }
                """;

        mockMvc.perform(post("/agendamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void confirmar_deveRetornarAgendamentoConfirmado() throws Exception {
        given(agendamentoService.confirmar(AGENDAMENTO_ID))
                .willReturn(agendamentoDTO(LocalDateTime.now().plusDays(1), StatusAgendamento.CONFIRMADO));

        mockMvc.perform(put("/agendamentos/{id}/confirmar", AGENDAMENTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    void cancelar_deveRetornarAgendamentoCancelado() throws Exception {
        given(agendamentoService.cancelar(AGENDAMENTO_ID))
                .willReturn(agendamentoDTO(LocalDateTime.now().plusDays(1), StatusAgendamento.CANCELADO));

        mockMvc.perform(put("/agendamentos/{id}/cancelar", AGENDAMENTO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    void reagendar_deveRepassarNovaDataAoService() throws Exception {
        LocalDateTime novaData = LocalDateTime.of(2027, 1, 6, 11, 0);
        given(agendamentoService.reagendar(eq(AGENDAMENTO_ID), any()))
                .willReturn(agendamentoDTO(novaData, StatusAgendamento.PENDENTE));

        mockMvc.perform(put("/agendamentos/{id}/reagendar", AGENDAMENTO_ID)
                        .param("novaData", "2027-01-06T11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        verify(agendamentoService).reagendar(eq(AGENDAMENTO_ID), eq(novaData));
    }
}
