package com.avantbarber.avant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.avantbarber.avant.dto.ServicoDesejadoDTO;
import com.avantbarber.avant.service.ServicoDesejadoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/servicos-desejados")
@RequiredArgsConstructor
public class ServicoDesejadoController {

    private final ServicoDesejadoService servicoDesejadoService;

    @GetMapping
    public List<ServicoDesejadoDTO> listarServicosDesejados() {
        return servicoDesejadoService.listarServicosDesejados();
    }

    @GetMapping("/{id}")
    public ServicoDesejadoDTO buscarPorId(@PathVariable Long id) {
        return servicoDesejadoService.buscarPorId(id);
    }

    @PostMapping
    public ServicoDesejadoDTO salvar(@RequestBody ServicoDesejadoDTO servicoDesejadoDTO) {
        return servicoDesejadoService.salvar(servicoDesejadoDTO);
    }

    @PutMapping("/{id}")
    public ServicoDesejadoDTO atualizar(@PathVariable Long id, @RequestBody ServicoDesejadoDTO servicoDesejadoDTO) {
        return servicoDesejadoService.atualizar(id, servicoDesejadoDTO);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        servicoDesejadoService.deletar(id);
    }
}
