package com.avantbarber.avant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.avantbarber.avant.dto.ServicoDesejadoDTO;
import com.avantbarber.avant.exception.BusinessException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.ServicoDesejado;
import com.avantbarber.avant.repository.ServicoDesejadoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicoDesejadoService {

    private static final int GRANULARIDADE_MINUTOS = 30;

    private final ServicoDesejadoRepository servicoDesejadoRepository;

    public List<ServicoDesejadoDTO> listarServicosDesejados() {
        return servicoDesejadoRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public ServicoDesejadoDTO buscarPorId(Long id) {
        return servicoDesejadoRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado com o ID: " + id));
    }  

    public ServicoDesejadoDTO salvar(ServicoDesejadoDTO servicoDesejadoDTO) {
        validarDuracao(servicoDesejadoDTO.getDuracaoMinutos());
        ServicoDesejado servicoDesejado = new ServicoDesejado();
        servicoDesejado.setNome(servicoDesejadoDTO.getNome());
        servicoDesejado.setPreco(servicoDesejadoDTO.getPreco());
        servicoDesejado.setDuracaoMinutos(servicoDesejadoDTO.getDuracaoMinutos());
        return toDTO(servicoDesejadoRepository.save(servicoDesejado));
    }

    public ServicoDesejadoDTO atualizar(Long id, ServicoDesejadoDTO servicoDesejadoDTO) {
        validarDuracao(servicoDesejadoDTO.getDuracaoMinutos());
        ServicoDesejado servicoDesejado = servicoDesejadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado com o ID: " + id));
        servicoDesejado.setNome(servicoDesejadoDTO.getNome());
        servicoDesejado.setPreco(servicoDesejadoDTO.getPreco());
        servicoDesejado.setDuracaoMinutos(servicoDesejadoDTO.getDuracaoMinutos());
        return toDTO(servicoDesejadoRepository.save(servicoDesejado));
    } 

    public void deletar(Long id) {
        servicoDesejadoRepository.deleteById(id);
    }

    private void validarDuracao(Integer duracaoMinutos) {
        if (duracaoMinutos == null || duracaoMinutos <= 0 || duracaoMinutos % GRANULARIDADE_MINUTOS != 0) {
            throw new BusinessException("Erro: A duração do serviço deve ser um múltiplo de "
                    + GRANULARIDADE_MINUTOS + " minutos maior que zero!");
        }
    }

    private ServicoDesejadoDTO toDTO(ServicoDesejado servicoDesejado) {
        return new ServicoDesejadoDTO(
                servicoDesejado.getId(),
                servicoDesejado.getNome(),
                servicoDesejado.getPreco(),
                servicoDesejado.getDuracaoMinutos()
        );
    }
}
