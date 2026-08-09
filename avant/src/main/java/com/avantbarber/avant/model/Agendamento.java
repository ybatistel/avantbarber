package com.avantbarber.avant.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "agendamento")
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime data;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private OrigemAgendamento origem;

    @ManyToOne
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "servico_id", nullable = false)
    private ServicoDesejado servico;
    
}
