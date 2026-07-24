package com.avantbarber.avant.repository;

import com.avantbarber.avant.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    Optional<Agendamento> findById (Long id);

    boolean existsByBarbeiroIdAndData(Long barbeiroId, LocalDateTime data);
    boolean existsByClienteIdAndData(Long clienteId, LocalDateTime data);
}
