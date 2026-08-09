package com.avantbarber.avant.dto;

import java.time.LocalDateTime;

import com.avantbarber.avant.model.OrigemAgendamento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoRequestDTO {
    private Long clienteId;
    private Long servicoId;
    private Long barbeiroId;
    private LocalDateTime dataHora;
    private String status;
    private OrigemAgendamento origem;

}
