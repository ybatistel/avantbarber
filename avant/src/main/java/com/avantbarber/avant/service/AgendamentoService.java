package com.avantbarber.avant.service;

import com.avantbarber.avant.dto.AgendamentoDTO;
import com.avantbarber.avant.dto.AgendamentoRequestDTO;
import com.avantbarber.avant.exception.BusinessException;
import com.avantbarber.avant.exception.HorarioFuncionamentoException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.Agendamento;
import com.avantbarber.avant.model.OrigemAgendamento;
import com.avantbarber.avant.model.StatusAgendamento;
import com.avantbarber.avant.model.ServicoDesejado;
import com.avantbarber.avant.repository.AgendamentoRepository;
import com.avantbarber.avant.repository.BarbeiroRepository;
import com.avantbarber.avant.repository.ClienteRepository;
import com.avantbarber.avant.repository.ServicoDesejadoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private static final int LIMITE_PENDENTES_AUTOMACAO = 3;

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoDesejadoRepository servicoDesejadoRepository;


    public List<AgendamentoDTO> listarAgendamentos() {
        return agendamentoRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private AgendamentoDTO toDTO(Agendamento agendamento) {
        return new AgendamentoDTO(
                agendamento.getId(),
                agendamento.getCliente().getId(),
                agendamento.getServico().getId(),
                agendamento.getBarbeiro().getId(),
                agendamento.getBarbeiro().getNome(),
                agendamento.getCliente().getNome(),
                agendamento.getServico().getNome(),
                agendamento.getServico().getPreco(),
                agendamento.getCliente().getCpf(),
                agendamento.getData(),
                agendamento.getStatus(),
                agendamento.getOrigem()
        );
    }


    private Agendamento toEntity(AgendamentoRequestDTO agendamentoRequestDTO) {
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(clienteRepository.findById(agendamentoRequestDTO.getClienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + agendamentoRequestDTO.getClienteId())));
        agendamento.setBarbeiro(barbeiroRepository.findById(agendamentoRequestDTO.getBarbeiroId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado com o ID: " + agendamentoRequestDTO.getBarbeiroId())));
        agendamento.setServico(servicoDesejadoRepository.findById(agendamentoRequestDTO.getServicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado com o ID: " + agendamentoRequestDTO.getServicoId())));
        agendamento.setData(agendamentoRequestDTO.getDataHora());
        agendamento.setOrigem(agendamentoRequestDTO.getOrigem() != null
                ? agendamentoRequestDTO.getOrigem()
                : OrigemAgendamento.MANUAL);

        return agendamento;
    }

    public AgendamentoDTO buscarPorId(Long id) {
        return agendamentoRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + id));
    }

    @Transactional
    public AgendamentoDTO salvar(AgendamentoRequestDTO agendamentoRequestDTO) {
        Agendamento agendamento = toEntity(agendamentoRequestDTO);
        int duracaoMinutos = agendamento.getServico().getDuracaoMinutos();
        validarDataRetroativa(agendamento.getData());
        validarHorarioFuncionamento(agendamento.getData());
        validarTerminoDentroDoExpediente(agendamento.getData(), duracaoMinutos);
        validarDisponibilidadeBarbeiro(agendamento.getBarbeiro().getId(), agendamento.getData(), duracaoMinutos, null);
        validarDisponibilidadeCliente(agendamento.getCliente().getId(), agendamento.getData(), duracaoMinutos, null);
        if (agendamento.getOrigem() == OrigemAgendamento.AUTOMACAO) {
            validarLimitePendentesAutomacao(agendamento.getCliente().getId());
        }
        agendamento.setStatus(StatusAgendamento.PENDENTE);
        return toDTO(agendamentoRepository.save(agendamento));
    }

    private void validarLimitePendentesAutomacao(Long clienteId) {
        // Trava o cliente até o fim da transação, serializando validações concorrentes
        // pro mesmo cliente e evitando que o limite seja ultrapassado.
        clienteRepository.findByIdComLock(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + clienteId));
        long pendentesViaAutomacao = agendamentoRepository.countByClienteIdAndStatusAndOrigem(
                clienteId, StatusAgendamento.PENDENTE, OrigemAgendamento.AUTOMACAO);
        if (pendentesViaAutomacao >= LIMITE_PENDENTES_AUTOMACAO) {
            throw new BusinessException("Erro: O cliente já possui o máximo de " + LIMITE_PENDENTES_AUTOMACAO
                    + " agendamentos pendentes criados por automação!");
        }
    }

    private void validarDataRetroativa(LocalDateTime dataAgendamento) {
        if (dataAgendamento.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Erro: Não é possível realizar um agendamento no passado!");
        }
    }

    // Fonte única do horário de funcionamento por dia da semana: validarHorarioFuncionamento,
    // validarTerminoDentroDoExpediente e listarHorariosDisponiveis dependem destes dois
    // métodos em vez de repetir os LocalTime de abertura/fechamento cada um por conta própria
    // — evita que os três voltem a divergir se o horário de algum dia mudar no futuro.
    private LocalTime obterAberturaExpediente(DayOfWeek diaSemana) {
        return switch (diaSemana) {
            case SUNDAY -> LocalTime.of(9, 0);
            case MONDAY -> LocalTime.of(13, 30);
            default -> LocalTime.of(10, 0);
        };
    }

    private LocalTime obterFechamentoExpediente(DayOfWeek diaSemana) {
        return switch (diaSemana) {
            case SUNDAY -> LocalTime.of(14, 0);
            case MONDAY -> LocalTime.of(19, 0);
            default -> LocalTime.of(19, 0);
        };
    }

    private void validarHorarioFuncionamento(LocalDateTime dataAgendamento) {
        LocalTime horaAgendamento = dataAgendamento.toLocalTime();
        DayOfWeek diaSemana = dataAgendamento.getDayOfWeek();

        if (diaSemana == DayOfWeek.SATURDAY) {
            throw new HorarioFuncionamentoException("Erro: Não é possível realizar agendamentos aos sábados!");
        }

        LocalTime abertura = obterAberturaExpediente(diaSemana);
        LocalTime fechamento = obterFechamentoExpediente(diaSemana);
        if (horaAgendamento.isBefore(abertura) || horaAgendamento.isAfter(fechamento)) {
            throw new HorarioFuncionamentoException(mensagemForaDoExpediente(diaSemana));
        }
    }

    private String mensagemForaDoExpediente(DayOfWeek diaSemana) {
        return switch (diaSemana) {
            case SUNDAY -> "Erro: Não é possível realizar agendamentos aos domingos fora do horário de funcionamento (9h às 14h)!";
            case MONDAY -> "Erro: Não é possível realizar agendamentos às segundas-feiras fora do horário de funcionamento (13h30 às 19h)!";
            default -> "Erro: Não é possível realizar agendamentos de terça a sexta fora do horário de funcionamento (10h às 19h)!";
        };
    }

    private void validarTerminoDentroDoExpediente(LocalDateTime dataAgendamento, int duracaoMinutos) {
        LocalTime fimExpediente = obterFechamentoExpediente(dataAgendamento.getDayOfWeek());
        LocalTime fimAgendamento = dataAgendamento.toLocalTime().plusMinutes(duracaoMinutos);
        if (fimAgendamento.isBefore(dataAgendamento.toLocalTime()) || fimAgendamento.isAfter(fimExpediente)) {
            throw new HorarioFuncionamentoException("Erro: A duração do serviço ultrapassa o horário de funcionamento!");
        }
    }

    private void validarDisponibilidadeBarbeiro(Long barbeiroId, LocalDateTime dataAgendamento, int duracaoMinutos, Long agendamentoIdExcluir) {
        List<Agendamento> agendamentosDoDia = agendamentoRepository.findByBarbeiroIdAndDataBetweenAndStatusNot(
                barbeiroId, dataAgendamento.toLocalDate().atStartOfDay(), dataAgendamento.toLocalDate().atTime(LocalTime.MAX), StatusAgendamento.CANCELADO);
        if (existeSobreposicao(agendamentosDoDia, dataAgendamento, duracaoMinutos, agendamentoIdExcluir)) {
            throw new BusinessException("Erro: O barbeiro já possui um agendamento nesse horário!");
        }
    }

    private void validarDisponibilidadeCliente(Long clienteId, LocalDateTime dataAgendamento, int duracaoMinutos, Long agendamentoIdExcluir) {
        List<Agendamento> agendamentosDoDia = agendamentoRepository.findByClienteIdAndDataBetweenAndStatusNot(
                clienteId, dataAgendamento.toLocalDate().atStartOfDay(), dataAgendamento.toLocalDate().atTime(LocalTime.MAX), StatusAgendamento.CANCELADO);
        if (existeSobreposicao(agendamentosDoDia, dataAgendamento, duracaoMinutos, agendamentoIdExcluir)) {
            throw new BusinessException("Erro: O cliente já possui um agendamento nesse horário!");
        }
    }

    private boolean existeSobreposicao(List<Agendamento> agendamentosDoDia, LocalDateTime novoInicio, int novaDuracaoMinutos, Long agendamentoIdExcluir) {
        LocalDateTime novoFim = novoInicio.plusMinutes(novaDuracaoMinutos);
        return agendamentosDoDia.stream()
                .filter(existente -> agendamentoIdExcluir == null || !existente.getId().equals(agendamentoIdExcluir))
                .anyMatch(existente -> {
                    LocalDateTime existenteInicio = existente.getData();
                    LocalDateTime existenteFim = existenteInicio.plusMinutes(existente.getServico().getDuracaoMinutos());
                    return existenteInicio.isBefore(novoFim) && existenteFim.isAfter(novoInicio);
                });
    }

    @Transactional
    public AgendamentoDTO confirmar(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + id));
        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new BusinessException("Erro: Só é possível confirmar um agendamento que esteja PENDENTE!");
        }
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        return toDTO(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoDTO cancelar(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + id));
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return toDTO(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoDTO reagendar(Long id, LocalDateTime novaDataAgendamento) {
        Agendamento reagendar = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + id));
        int duracaoMinutos = reagendar.getServico().getDuracaoMinutos();
        validarDataRetroativa(novaDataAgendamento);
        validarHorarioFuncionamento(novaDataAgendamento);
        validarTerminoDentroDoExpediente(novaDataAgendamento, duracaoMinutos);
        validarDisponibilidadeCliente(reagendar.getCliente().getId(), novaDataAgendamento, duracaoMinutos, reagendar.getId());
        validarDisponibilidadeBarbeiro(reagendar.getBarbeiro().getId(), novaDataAgendamento, duracaoMinutos, reagendar.getId());
        reagendar.setData(novaDataAgendamento);
        reagendar.setStatus(StatusAgendamento.PENDENTE);
        return toDTO(agendamentoRepository.save(reagendar));
    }

    public List<LocalTime> listarHorariosDisponiveis(Long barbeiroId, LocalDate dataAgendamento, Long servicoId) {

        // valida barbeiro
        if (!barbeiroRepository.existsById(barbeiroId)) {
            throw new RecursoNaoEncontradoException("Barbeiro não encontrado com o ID: " + barbeiroId);
        }

        // define abertura e fechamento da barbearia com base no dia da semana
        DayOfWeek dia = dataAgendamento.getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY) {
            return Collections.emptyList();
        }

        ServicoDesejado servico = servicoDesejadoRepository.findById(servicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado com o ID: " + servicoId));
        int duracaoMinutos = servico.getDuracaoMinutos();

        LocalTime inicioDia = obterAberturaExpediente(dia);
        LocalTime fimDia = obterFechamentoExpediente(dia);

        // Busca o que já está ocupado no banco
        LocalDateTime dataInicio = dataAgendamento.atStartOfDay();
        LocalDateTime dataFim = dataAgendamento.atTime(LocalTime.MAX);

        List<Agendamento> agendamentosOcupados = agendamentoRepository
                .findByBarbeiroIdAndDataBetweenAndStatusNot(barbeiroId, dataInicio, dataFim, StatusAgendamento.CANCELADO);

        // Monta a lista de horários livres de 30 em 30 minutos, considerando a duração do
        // serviço consultado: um horário só é oferecido se o serviço inteiro couber antes
        // do fechamento e não sobrepuser nenhum agendamento já ocupado (que, por sua vez,
        // ocupa a duração do SEU PRÓPRIO serviço, não a do serviço consultado).
        List<LocalTime> horasDisponiveis = new ArrayList<>();
        LocalTime horarioAtual = inicioDia;

        while (!horarioAtual.isAfter(fimDia.minusMinutes(30))) {
            LocalDateTime inicioCandidato = dataAgendamento.atTime(horarioAtual);
            LocalDateTime fimCandidato = inicioCandidato.plusMinutes(duracaoMinutos);

            boolean terminaDentroDoExpediente = !fimCandidato.toLocalTime().isAfter(fimDia)
                    && !fimCandidato.toLocalTime().isBefore(horarioAtual);
            boolean estaOcupado = existeSobreposicao(agendamentosOcupados, inicioCandidato, duracaoMinutos, null);
            boolean eNoPassado = dataAgendamento.isEqual(LocalDate.now()) && horarioAtual.isBefore(LocalTime.now());

            if (terminaDentroDoExpediente && !estaOcupado && !eNoPassado) {
                horasDisponiveis.add(horarioAtual);
            }
            horarioAtual = horarioAtual.plusMinutes(30); // Avança 30 minutos
        }
        return horasDisponiveis; // Retornar a lista final
    }
}