package com.avantbarber.avant.repository;

import com.avantbarber.avant.model.Agendamento;
import com.avantbarber.avant.model.OrigemAgendamento;
import com.avantbarber.avant.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    Optional<Agendamento> findById (Long id);
    List<Agendamento> findByBarbeiroIdAndDataBetweenAndStatusNot(
            Long barbeiroId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
            StatusAgendamento statusAgendamento);

    boolean existsByBarbeiroIdAndData(Long barbeiroId, LocalDateTime data);
    boolean existsByClienteIdAndData(Long clienteId, LocalDateTime data);
    boolean existsByData(LocalDateTime data);
    long countByClienteIdAndStatusAndOrigem(Long clienteId, StatusAgendamento status, OrigemAgendamento origem);
}
