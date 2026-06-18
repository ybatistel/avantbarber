package com.avantbarber.avant.repository;

import com.avantbarber.avant.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> id(Long id);
}
