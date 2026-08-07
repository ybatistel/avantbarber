package com.avantbarber.avant.service;

import com.avantbarber.avant.dto.AgendamentoDTO;
import com.avantbarber.avant.dto.AgendamentoRequestDTO;
import com.avantbarber.avant.exception.BusinessException;
import com.avantbarber.avant.exception.HorarioFuncionamentoException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.Agendamento;
import com.avantbarber.avant.model.Barbeiro;
import com.avantbarber.avant.model.Cliente;
import com.avantbarber.avant.model.Perfil;
import com.avantbarber.avant.model.ServicoDesejado;
import com.avantbarber.avant.model.StatusAgendamento;
import com.avantbarber.avant.repository.AgendamentoRepository;
import com.avantbarber.avant.repository.BarbeiroRepository;
import com.avantbarber.avant.repository.ClienteRepository;
import com.avantbarber.avant.repository.ServicoDesejadoRepository;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private BarbeiroRepository barbeiroRepository;
    @Mock
    private ServicoDesejadoRepository servicoDesejadoRepository;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private static final Long CLIENTE_ID = 1L;
    private static final Long BARBEIRO_ID = 1L;
    private static final Long SERVICO_ID = 1L;

    // Sempre 1 ano no futuro para nunca cair em "agendamento no passado" e para
    // controlar o dia da semana independente de quando o teste rodar.
    private static LocalDateTime proximaData(DayOfWeek diaSemana, LocalTime hora) {
        return LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(diaSemana)).atTime(hora);
    }

    private static Cliente cliente(Long id) {
        return Cliente.builder()
                .id(id).nome("Cliente Teste").cpf("12345678900")
                .numero("11999999999").senha("senha").endereco("Rua Teste, 123")
                .build();
    }

    private static Barbeiro barbeiro(Long id) {
        return Barbeiro.builder()
                .id(id).nome("Barbeiro Teste").numero("11988888888")
                .cpf("98765432100").senha("senha").perfil(Perfil.BARBEIRO)
                .build();
    }

    private static ServicoDesejado servico(Long id) {
        return new ServicoDesejado(id, "Corte", BigDecimal.valueOf(50));
    }

    private static AgendamentoRequestDTO requestDTO(LocalDateTime data) {
        return new AgendamentoRequestDTO(CLIENTE_ID, SERVICO_ID, BARBEIRO_ID, data, null);
    }

    private static Agendamento agendamentoExistente(Long id, LocalDateTime data, StatusAgendamento status) {
        return Agendamento.builder()
                .id(id).data(data).status(status)
                .cliente(cliente(CLIENTE_ID)).barbeiro(barbeiro(BARBEIRO_ID)).servico(servico(SERVICO_ID))
                .build();
    }

    private void mockarEntidadesRelacionadas() {
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID)));
        when(barbeiroRepository.findById(BARBEIRO_ID)).thenReturn(Optional.of(barbeiro(BARBEIRO_ID)));
        when(servicoDesejadoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico(SERVICO_ID)));
    }

    private void mockarSalvarComEcho() {
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- salvar ----------

    @Test
    void salvar_deveCriarAgendamentoComStatusPendente_quandoDadosValidos() {
        LocalDateTime data = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        mockarEntidadesRelacionadas();
        when(agendamentoRepository.existsByBarbeiroIdAndData(BARBEIRO_ID, data)).thenReturn(false);
        when(agendamentoRepository.existsByClienteIdAndData(CLIENTE_ID, data)).thenReturn(false);
        mockarSalvarComEcho();

        AgendamentoDTO resultado = agendamentoService.salvar(requestDTO(data));

        assertThat(resultado.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
        assertThat(resultado.getDataHora()).isEqualTo(data);
        verify(agendamentoRepository).save(any(Agendamento.class));
    }

    @Test
    void salvar_deveLancarBusinessException_quandoDataNoPassado() {
        mockarEntidadesRelacionadas();
        LocalDateTime dataPassada = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> agendamentoService.salvar(requestDTO(dataPassada)))
                .isInstanceOf(BusinessException.class);

        verify(agendamentoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "{0} às {1} deve ser rejeitado")
    @MethodSource("horariosForaDoExpediente")
    void salvar_deveLancarHorarioFuncionamentoException_quandoForaDoExpediente(DayOfWeek dia, LocalTime hora) {
        mockarEntidadesRelacionadas();
        LocalDateTime data = proximaData(dia, hora);

        assertThatThrownBy(() -> agendamentoService.salvar(requestDTO(data)))
                .isInstanceOf(HorarioFuncionamentoException.class);

        verify(agendamentoRepository, never()).save(any());
    }

    static Stream<Arguments> horariosForaDoExpediente() {
        return Stream.of(
                Arguments.of(DayOfWeek.SATURDAY, LocalTime.of(12, 0)),
                Arguments.of(DayOfWeek.SUNDAY, LocalTime.of(8, 59)),
                Arguments.of(DayOfWeek.SUNDAY, LocalTime.of(14, 1)),
                Arguments.of(DayOfWeek.MONDAY, LocalTime.of(13, 29)),
                Arguments.of(DayOfWeek.MONDAY, LocalTime.of(19, 1)),
                Arguments.of(DayOfWeek.TUESDAY, LocalTime.of(9, 59)),
                Arguments.of(DayOfWeek.TUESDAY, LocalTime.of(19, 1)),
                Arguments.of(DayOfWeek.FRIDAY, LocalTime.of(19, 1))
        );
    }

    @ParameterizedTest(name = "{0} às {1} (limite do expediente) deve ser aceito")
    @MethodSource("horariosNoLimiteDoExpediente")
    void salvar_devePermitir_quandoExatamenteNoLimiteDoExpediente(DayOfWeek dia, LocalTime hora) {
        mockarEntidadesRelacionadas();
        LocalDateTime data = proximaData(dia, hora);
        when(agendamentoRepository.existsByBarbeiroIdAndData(BARBEIRO_ID, data)).thenReturn(false);
        when(agendamentoRepository.existsByClienteIdAndData(CLIENTE_ID, data)).thenReturn(false);
        mockarSalvarComEcho();

        AgendamentoDTO resultado = agendamentoService.salvar(requestDTO(data));

        assertThat(resultado.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
    }

    static Stream<Arguments> horariosNoLimiteDoExpediente() {
        return Stream.of(
                Arguments.of(DayOfWeek.SUNDAY, LocalTime.of(9, 0)),
                Arguments.of(DayOfWeek.SUNDAY, LocalTime.of(14, 0)),
                Arguments.of(DayOfWeek.MONDAY, LocalTime.of(13, 30)),
                Arguments.of(DayOfWeek.MONDAY, LocalTime.of(19, 0)),
                Arguments.of(DayOfWeek.TUESDAY, LocalTime.of(10, 0)),
                Arguments.of(DayOfWeek.FRIDAY, LocalTime.of(19, 0))
        );
    }

    @Test
    void salvar_deveLancarBusinessException_quandoBarbeiroJaTemAgendamentoNoHorario() {
        LocalDateTime data = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        mockarEntidadesRelacionadas();
        when(agendamentoRepository.existsByBarbeiroIdAndData(BARBEIRO_ID, data)).thenReturn(true);

        assertThatThrownBy(() -> agendamentoService.salvar(requestDTO(data)))
                .isInstanceOf(BusinessException.class);

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void salvar_deveLancarBusinessException_quandoClienteJaTemAgendamentoNoHorario() {
        LocalDateTime data = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        mockarEntidadesRelacionadas();
        when(agendamentoRepository.existsByBarbeiroIdAndData(BARBEIRO_ID, data)).thenReturn(false);
        when(agendamentoRepository.existsByClienteIdAndData(CLIENTE_ID, data)).thenReturn(true);

        assertThatThrownBy(() -> agendamentoService.salvar(requestDTO(data)))
                .isInstanceOf(BusinessException.class);

        verify(agendamentoRepository, never()).save(any());
    }

    // ---------- cancelar ----------

    @Test
    void cancelar_deveAlterarStatusParaCancelado_quandoAgendamentoExiste() {
        LocalDateTime data = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        Agendamento existente = agendamentoExistente(1L, data, StatusAgendamento.PENDENTE);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        mockarSalvarComEcho();

        AgendamentoDTO resultado = agendamentoService.cancelar(1L);

        assertThat(resultado.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    void cancelar_deveLancarRecursoNaoEncontradoException_quandoIdNaoExiste() {
        when(agendamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agendamentoService.cancelar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- reagendar ----------

    @Test
    void reagendar_deveAtualizarDataEStatusPendente_quandoDadosValidos() {
        LocalDateTime dataAntiga = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        LocalDateTime novaData = proximaData(DayOfWeek.WEDNESDAY, LocalTime.of(11, 0));
        Agendamento existente = agendamentoExistente(1L, dataAntiga, StatusAgendamento.CONFIRMADO);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(agendamentoRepository.existsByClienteIdAndData(CLIENTE_ID, novaData)).thenReturn(false);
        when(agendamentoRepository.existsByBarbeiroIdAndData(BARBEIRO_ID, novaData)).thenReturn(false);
        mockarSalvarComEcho();

        AgendamentoDTO resultado = agendamentoService.reagendar(1L, novaData);

        assertThat(resultado.getDataHora()).isEqualTo(novaData);
        assertThat(resultado.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
    }

    @Test
    void reagendar_deveLancarRecursoNaoEncontradoException_quandoIdNaoExiste() {
        when(agendamentoRepository.findById(99L)).thenReturn(Optional.empty());
        LocalDateTime novaData = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));

        assertThatThrownBy(() -> agendamentoService.reagendar(99L, novaData))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void reagendar_deveLancarBusinessException_quandoNovaDataNoPassado() {
        LocalDateTime dataAntiga = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        Agendamento existente = agendamentoExistente(1L, dataAntiga, StatusAgendamento.CONFIRMADO);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        LocalDateTime dataPassada = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> agendamentoService.reagendar(1L, dataPassada))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reagendar_deveLancarHorarioFuncionamentoException_quandoNovaDataForaDoExpediente() {
        LocalDateTime dataAntiga = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        Agendamento existente = agendamentoExistente(1L, dataAntiga, StatusAgendamento.CONFIRMADO);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        LocalDateTime foraDoExpediente = proximaData(DayOfWeek.SATURDAY, LocalTime.of(12, 0));

        assertThatThrownBy(() -> agendamentoService.reagendar(1L, foraDoExpediente))
                .isInstanceOf(HorarioFuncionamentoException.class);
    }

    @Test
    void reagendar_deveLancarBusinessException_quandoClienteJaTemAgendamentoNoNovoHorario() {
        LocalDateTime dataAntiga = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        LocalDateTime novaData = proximaData(DayOfWeek.WEDNESDAY, LocalTime.of(11, 0));
        Agendamento existente = agendamentoExistente(1L, dataAntiga, StatusAgendamento.CONFIRMADO);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(agendamentoRepository.existsByClienteIdAndData(CLIENTE_ID, novaData)).thenReturn(true);

        assertThatThrownBy(() -> agendamentoService.reagendar(1L, novaData))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reagendar_deveLancarBusinessException_quandoBarbeiroJaTemAgendamentoNoNovoHorario() {
        LocalDateTime dataAntiga = proximaData(DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        LocalDateTime novaData = proximaData(DayOfWeek.WEDNESDAY, LocalTime.of(11, 0));
        Agendamento existente = agendamentoExistente(1L, dataAntiga, StatusAgendamento.CONFIRMADO);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(agendamentoRepository.existsByClienteIdAndData(CLIENTE_ID, novaData)).thenReturn(false);
        when(agendamentoRepository.existsByBarbeiroIdAndData(BARBEIRO_ID, novaData)).thenReturn(true);

        assertThatThrownBy(() -> agendamentoService.reagendar(1L, novaData))
                .isInstanceOf(BusinessException.class);
    }

    // ---------- listarHorariosDisponiveis ----------

    @Test
    void listarHorariosDisponiveis_deveLancarRecursoNaoEncontradoException_quandoBarbeiroNaoExiste() {
        when(barbeiroRepository.existsById(BARBEIRO_ID)).thenReturn(false);
        LocalDate data = LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));

        assertThatThrownBy(() -> agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, data))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarHorariosDisponiveis_deveRetornarListaVazia_quandoSabado() {
        when(barbeiroRepository.existsById(BARBEIRO_ID)).thenReturn(true);
        LocalDate sabado = LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        List<LocalTime> resultado = agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, sabado);

        assertThat(resultado).isEmpty();
    }

    @Test
    void listarHorariosDisponiveis_deveUsarJanelaCorreta_quandoDomingo() {
        when(barbeiroRepository.existsById(BARBEIRO_ID)).thenReturn(true);
        LocalDate domingo = LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusNot(
                eq(BARBEIRO_ID), any(), any(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(List.of());

        List<LocalTime> resultado = agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, domingo);

        assertThat(resultado).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalTime.of(11, 0), LocalTime.of(11, 30), LocalTime.of(12, 0), LocalTime.of(12, 30),
                LocalTime.of(13, 0), LocalTime.of(13, 30)
        );
    }

    @Test
    void listarHorariosDisponiveis_deveUsarJanelaCorreta_quandoSegunda() {
        when(barbeiroRepository.existsById(BARBEIRO_ID)).thenReturn(true);
        LocalDate segunda = LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusNot(
                eq(BARBEIRO_ID), any(), any(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(List.of());

        List<LocalTime> resultado = agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, segunda);

        assertThat(resultado).first().isEqualTo(LocalTime.of(13, 30));
        assertThat(resultado).last().isEqualTo(LocalTime.of(18, 30));
        assertThat(resultado).hasSize(11);
    }

    @Test
    void listarHorariosDisponiveis_deveUsarJanelaCorreta_quandoTercaASexta() {
        when(barbeiroRepository.existsById(BARBEIRO_ID)).thenReturn(true);
        LocalDate terca = LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusNot(
                eq(BARBEIRO_ID), any(), any(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(List.of());

        List<LocalTime> resultado = agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, terca);

        assertThat(resultado).first().isEqualTo(LocalTime.of(10, 0));
        assertThat(resultado).last().isEqualTo(LocalTime.of(18, 30));
        assertThat(resultado).hasSize(18);
    }

    @Test
    void listarHorariosDisponiveis_deveExcluirSlotOcupado() {
        when(barbeiroRepository.existsById(BARBEIRO_ID)).thenReturn(true);
        LocalDate terca = LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        Agendamento ocupando10h = agendamentoExistente(2L, terca.atTime(10, 0), StatusAgendamento.CONFIRMADO);
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusNot(
                eq(BARBEIRO_ID), any(), any(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(List.of(ocupando10h));

        List<LocalTime> resultado = agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, terca);

        assertThat(resultado).doesNotContain(LocalTime.of(10, 0));
        assertThat(resultado).contains(LocalTime.of(10, 30));
    }

    @Test
    void listarHorariosDisponiveis_deveConsultarApenasAgendamentosNaoCancelados() {
        // A exclusão de agendamentos CANCELADO acontece na query derivada do repositório
        // (existsByBarbeiroIdAndDataBetweenAndStatusNot), não em código do service — aqui
        // confirmamos apenas que o service pede a exclusão certa; o filtro em si é
        // responsabilidade do Spring Data / banco (fora do escopo de um teste de unidade).
        when(barbeiroRepository.existsById(BARBEIRO_ID)).thenReturn(true);
        LocalDate terca = LocalDate.now().plusYears(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusNot(
                eq(BARBEIRO_ID), any(), any(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(List.of());

        agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, terca);

        verify(agendamentoRepository).findByBarbeiroIdAndDataBetweenAndStatusNot(
                eq(BARBEIRO_ID), any(), any(), eq(StatusAgendamento.CANCELADO));
    }

    @Test
    void listarHorariosDisponiveis_deveExcluirHorariosJaPassados_quandoDataEhHoje() {
        // Não há Clock injetado no AgendamentoService (LocalTime.now()/LocalDate.now() são
        // chamados direto), então este teste não consegue controlar "agora" — ele se auto-
        // ajusta ao horário real de execução e pula quando o cenário não é válido (fora do
        // expediente de hoje). É uma limitação de testabilidade da produção, não da regra.
        LocalDate hoje = LocalDate.now();
        DayOfWeek diaSemana = hoje.getDayOfWeek();
        Assumptions.assumeTrue(diaSemana != DayOfWeek.SATURDAY, "Sábado é fechado, cenário não aplicável hoje");

        LocalTime inicioDia;
        LocalTime fimDia;
        switch (diaSemana) {
            case SUNDAY -> { inicioDia = LocalTime.of(9, 0); fimDia = LocalTime.of(14, 0); }
            case MONDAY -> { inicioDia = LocalTime.of(13, 30); fimDia = LocalTime.of(19, 0); }
            default -> { inicioDia = LocalTime.of(10, 0); fimDia = LocalTime.of(19, 0); }
        }

        LocalTime agora = LocalTime.now();
        Assumptions.assumeTrue(agora.isAfter(inicioDia.plusMinutes(30)) && agora.isBefore(fimDia),
                "Teste só é válido dentro do expediente de hoje, com ao menos um slot já passado");

        when(barbeiroRepository.existsById(BARBEIRO_ID)).thenReturn(true);
        when(agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusNot(
                eq(BARBEIRO_ID), any(), any(), eq(StatusAgendamento.CANCELADO)))
                .thenReturn(List.of());

        List<LocalTime> resultado = agendamentoService.listarHorariosDisponiveis(BARBEIRO_ID, hoje);

        assertThat(resultado).doesNotContain(inicioDia);
        assertThat(resultado).allMatch(horario -> !horario.isBefore(agora));
    }
}
