package com.avantbarber.avant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.avantbarber.avant.model.Agendamento;
import com.avantbarber.avant.model.OrigemAgendamento;
import com.avantbarber.avant.model.StatusAgendamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoDTO {
    private Long id;
    private Long clienteId;
    private Long servicoId;
    private Long barbeiroId;
    private String nomeBarbeiro;
    private String nomeCliente;
    private String nomeServico;
    private BigDecimal precoServico;
    private String cpfCliente;
    private LocalDateTime dataHora;
    private StatusAgendamento status;
    private OrigemAgendamento origem;

    public AgendamentoDTO(Agendamento agendamento) {
        this.id = agendamento.getId();
        this.clienteId = agendamento.getCliente().getId();
        this.servicoId = agendamento.getServico().getId();
        this.barbeiroId = agendamento.getBarbeiro().getId();
        this.nomeBarbeiro = agendamento.getBarbeiro().getNome();
        this.nomeCliente = agendamento.getCliente().getNome();
        this.nomeServico = agendamento.getServico().getNome();
        this.precoServico = agendamento.getServico().getPreco();
        this.cpfCliente = agendamento.getCliente().getCpf();
        this.dataHora = agendamento.getData();
        this.status = agendamento.getStatus();
        this.origem = agendamento.getOrigem();
    }
}
