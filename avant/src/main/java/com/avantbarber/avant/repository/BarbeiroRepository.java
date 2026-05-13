package com.avantbarber.avant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.avantbarber.avant.model.Barbeiro;

@Repository
public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {
    
}
