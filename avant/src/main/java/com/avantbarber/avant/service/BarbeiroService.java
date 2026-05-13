package com.avantbarber.avant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.avantbarber.avant.model.Barbeiro;
import com.avantbarber.avant.repository.BarbeiroRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;

    public List<Barbeiro> listarBarbeiros() {
        return barbeiroRepository.findAll();
    }
    
    public Barbeiro buscarPorId(Long id) {
        return barbeiroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado com o ID: " + id));
    }
    public Barbeiro salvar(Barbeiro barbeiro) {
        return barbeiroRepository.save(barbeiro);
    }
    public Barbeiro atualizar(Long id, Barbeiro barbeiro) {
        Barbeiro barbeiroExistente = barbeiroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado com o ID: " + id));
        barbeiroExistente.setNome(barbeiro.getNome());
        return barbeiroRepository.save(barbeiroExistente);
    }
    public void deletar(Long id) {
        barbeiroRepository.deleteById(id);
    }
}
