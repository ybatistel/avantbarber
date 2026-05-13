package com.avantbarber.avant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.avantbarber.avant.dto.ServicoDesejadoDTO;
import com.avantbarber.avant.model.ServicoDesejado;
import com.avantbarber.avant.repository.ServicoDesejadoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicoDesejadoService {

    private final ServicoDesejadoRepository servicoDesejadoRepository;

    public List<ServicoDesejadoDTO> listarServicosDesejados() {
        return servicoDesejadoRepository.findAll().stream()
                .map(servico -> new ServicoDesejadoDTO(
                        servico.getId(),
                        servico.getNome(),
                        servico.getPreco()
                ))
                .toList();
    }

    public ServicoDesejadoDTO buscarPorId(Long id) {
        return servicoDesejadoRepository.findById(id)
                .map(servico -> new ServicoDesejadoDTO(
                        servico.getId(),
                        servico.getNome(),
                        servico.getPreco()
                ))
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado com o ID: " + id));
    }

    public ServicoDesejadoDTO salvar(ServicoDesejadoDTO servicoDesejadoDTO) {
        ServicoDesejado servicoDesejado = new ServicoDesejado();
        servicoDesejado.setNome(servicoDesejadoDTO.getNome());
        servicoDesejado.setPreco(servicoDesejadoDTO.getPreco());
        ServicoDesejado savedServicoDesejado = servicoDesejadoRepository.save(servicoDesejado);
        return new ServicoDesejadoDTO(savedServicoDesejado.getId(), savedServicoDesejado.getNome(), savedServicoDesejado.getPreco());
    }

    public ServicoDesejadoDTO atualizar(Long id, ServicoDesejadoDTO servicoDesejadoDTO) {
        ServicoDesejado servicoDesejado = servicoDesejadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado com o ID: " + id));
        servicoDesejado.setNome(servicoDesejadoDTO.getNome());
        servicoDesejado.setPreco(servicoDesejadoDTO.getPreco());
        ServicoDesejado updatedServicoDesejado = servicoDesejadoRepository.save(servicoDesejado);
        return new ServicoDesejadoDTO(updatedServicoDesejado.getId(), updatedServicoDesejado.getNome(), updatedServicoDesejado.getPreco());
    }

    public void deletar(Long id) {
        servicoDesejadoRepository.deleteById(id);
    }
}
