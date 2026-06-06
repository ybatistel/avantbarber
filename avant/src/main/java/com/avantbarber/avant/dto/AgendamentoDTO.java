package com.avantbarber.avant.dto;

import java.time.LocalDateTime;

import com.avantbarber.avant.model.Agendamento;
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
    private Double precoServico;
    private String cpfCliente;
    private LocalDateTime dataHora;
    private String status;

    public AgendamentoDTO(Agendamento agendamento) {
    }
}
