package com.avantbarber.avant.service;

import java.util.List;

import com.avantbarber.avant.exception.ChaveDuplicadaException;
import org.springframework.stereotype.Service;

import com.avantbarber.avant.dto.BarbeiroDTO;
import com.avantbarber.avant.dto.BarbeiroRequestDTO;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.Barbeiro;
import com.avantbarber.avant.repository.BarbeiroRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;

    public List<BarbeiroDTO> listarBarbeiros() {
        return barbeiroRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    private BarbeiroDTO toDTO(Barbeiro barbeiro) {
        return new BarbeiroDTO(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getNumero(),
                barbeiro.getCpf(),
                barbeiro.getPerfil());
    }

    private Barbeiro toEntity(BarbeiroRequestDTO barbeiroRequestDTO) {
        Barbeiro barbeiro = new Barbeiro();
        barbeiro.setNome(barbeiroRequestDTO.getNome());
        barbeiro.setNumero(barbeiroRequestDTO.getNumero());
        barbeiro.setCpf(barbeiroRequestDTO.getCpf());
        barbeiro.setSenha(barbeiroRequestDTO.getSenha());
        barbeiro.setPerfil(barbeiroRequestDTO.getPerfil());
        return barbeiro;
    }

    public BarbeiroDTO buscarPorId(Long id) {
        return barbeiroRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado com o ID: " + id));
    }

    public BarbeiroDTO salvar(BarbeiroRequestDTO barbeiroRequestDTO) {
        Barbeiro barbeiro = toEntity(barbeiroRequestDTO);
        if (barbeiroRepository.findByCpf(barbeiro.getCpf()).isPresent()) {
            throw new ChaveDuplicadaException("Erro: Já existe um barbeiro com CPF informado!");
        }
        Barbeiro savedBarbeiro = barbeiroRepository.save(barbeiro);
        return toDTO(savedBarbeiro);
    }
   
    public BarbeiroDTO atualizar(Long id, BarbeiroRequestDTO barbeiroRequestDTO) {
        Barbeiro barbeiro = barbeiroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbeiro não encontrado com o ID: " + id));
        barbeiro.setNome(barbeiroRequestDTO.getNome());
        barbeiro.setNumero(barbeiroRequestDTO.getNumero());
        barbeiro.setCpf(barbeiroRequestDTO.getCpf());
        barbeiro.setSenha(barbeiroRequestDTO.getSenha());
        barbeiro.setPerfil(barbeiroRequestDTO.getPerfil());
        Barbeiro updatedBarbeiro = barbeiroRepository.save(barbeiro);
        return toDTO(updatedBarbeiro);
    }

    public void deletar(Long id) {
        barbeiroRepository.deleteById(id);
    }
   
}
