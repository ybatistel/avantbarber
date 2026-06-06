package com.avantbarber.avant.service;

import com.avantbarber.avant.dto.AgendamentoDTO;
import com.avantbarber.avant.dto.AgendamentoRequestDTO;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.Agendamento;
import com.avantbarber.avant.repository.AgendamentoRepository;
import com.avantbarber.avant.repository.BarbeiroRepository;
import com.avantbarber.avant.repository.ClienteRepository;
import com.avantbarber.avant.repository.ServicoDesejadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
                agendamento.getStatus());
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
}
