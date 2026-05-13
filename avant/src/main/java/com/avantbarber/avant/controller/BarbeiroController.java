package com.avantbarber.avant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.avantbarber.avant.dto.BarbeiroDTO;
import com.avantbarber.avant.dto.BarbeiroRequestDTO;
import com.avantbarber.avant.service.BarbeiroService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService barbeiroService;

    @GetMapping
    public ResponseEntity<List<BarbeiroDTO>> listarBarbeiros() {
        return ResponseEntity.ok(barbeiroService.listarBarbeiros());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarbeiroDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(barbeiroService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<BarbeiroDTO> salvar(@RequestBody BarbeiroRequestDTO barbeiroRequestDTO) {
        return ResponseEntity.ok(barbeiroService.salvar(barbeiroRequestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarbeiroDTO> atualizar(@PathVariable Long id,
            @RequestBody BarbeiroRequestDTO barbeiroRequestDTO) {
        return ResponseEntity.ok(barbeiroService.atualizar(id, barbeiroRequestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        barbeiroService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
