package com.avantbarber.avant.service;

import com.avantbarber.avant.dto.AgendamentoDTO;
import com.avantbarber.avant.dto.AgendamentoRequestDTO;
import com.avantbarber.avant.exception.HorarioFuncionamentoException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.Agendamento;
import com.avantbarber.avant.model.StatusAgendamento;
import com.avantbarber.avant.repository.AgendamentoRepository;
import com.avantbarber.avant.repository.BarbeiroRepository;
import com.avantbarber.avant.repository.ClienteRepository;
import com.avantbarber.avant.repository.ServicoDesejadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ServicoDesejadoRepository  servicoDesejadoRepository;


    public List<AgendamentoDTO> listarAgendamentos(){
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
                agendamento.getStatus()
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

        return agendamento;
    }

    public AgendamentoDTO buscarPorId(Long id) {
        return agendamentoRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + id));
    }

    public AgendamentoDTO salvar(AgendamentoRequestDTO agendamentoRequestDTO) {
        Agendamento agendamento = toEntity(agendamentoRequestDTO);
        validarDataRetroativa(agendamento.getData());
        validarHorarioFuncionamento(agendamento.getData());
        validarDisponibilidadeBarbeiro(agendamento.getBarbeiro().getId(),agendamento.getData());
        validarDisponibilidadeCliente(agendamento.getCliente().getId(),agendamento.getData());
        agendamento.setStatus(StatusAgendamento.PENDENTE);
        return toDTO(agendamentoRepository.save(agendamento));
    }
    // testando apenas, novamente testando apenas
    private void validarDataRetroativa(LocalDateTime dataAgendamento) {
        if (dataAgendamento.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException ("Erro: Não é possível realizar um agendamento no passado!");
        }
    }

    private void validarHorarioFuncionamento(LocalDateTime dataAgendamento) {
        LocalTime horaAgendamento = dataAgendamento.toLocalTime();
        DayOfWeek diaSemana = dataAgendamento.getDayOfWeek();

        switch (diaSemana) {
            case SATURDAY -> throw new HorarioFuncionamentoException("Erro: Não é possível realizar agendamentos aos sábados!");

            case SUNDAY -> {
                if (horaAgendamento.isBefore(LocalTime.of(9, 0)) || horaAgendamento.isAfter(LocalTime.of(14, 0))) {
                    throw new HorarioFuncionamentoException("Erro: Não é possível realizar agendamentos aos domingos fora do horário de funcionamento (9h às 14h)!");
                }
            }

            case MONDAY -> {
                if (horaAgendamento.isBefore(LocalTime.of(13,30)) || horaAgendamento.isAfter(LocalTime.of(19,0))) {
                    throw new HorarioFuncionamentoException("Erro: Não é possível realizar agendamentos às segundas-feiras fora do horário de funcionamento (13h30 às 19h)!");
                }
            }

            case TUESDAY, WEDNESDAY,  THURSDAY, FRIDAY -> {
                if (horaAgendamento.isBefore(LocalTime.of(10,0)) || horaAgendamento.isAfter(LocalTime.of(19,0))) {
                    throw new HorarioFuncionamentoException("Erro: Não é possível realizar agendamentos de terça a sexta fora do horário de funcionamento (10h às 19h)!");
                }
            }
        }
    }

    private void validarDisponibilidadeBarbeiro(Long barbeiroId, LocalDateTime dataAgendamento) {
        if (agendamentoRepository.existsByBarbeiroIdAndData(barbeiroId, dataAgendamento)) {
            throw new IllegalArgumentException("Erro: O barbeiro já possui um agendamento nesse horário!");
        }
    }

    private void validarDisponibilidadeCliente(Long clienteId, LocalDateTime dataAgendamento) {
        if (agendamentoRepository.existsByClienteIdAndData(clienteId, dataAgendamento)) {
            throw new IllegalArgumentException("Erro: O cliente já possui um agendamento nesse horário!");
        }
    }

    public AgendamentoDTO cancelar(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + id));
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        return toDTO(agendamentoRepository.save(agendamento));
    }

    public AgendamentoDTO reagendar(Long id, LocalDateTime novaDataAgendamento) {
        Agendamento reagendar = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com o ID: " + id));
        validarDataRetroativa(novaDataAgendamento);
        validarHorarioFuncionamento(novaDataAgendamento);
        validarDisponibilidadeCliente(reagendar.getCliente().getId(), novaDataAgendamento);
        validarDisponibilidadeBarbeiro(reagendar.getBarbeiro().getId(), novaDataAgendamento);
        reagendar.setData(novaDataAgendamento);
        reagendar.setStatus(StatusAgendamento.PENDENTE);
        return toDTO(agendamentoRepository.save(reagendar));
    }
}

