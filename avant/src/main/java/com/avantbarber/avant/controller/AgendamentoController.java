package com.avantbarber.avant.controller;

import com.avantbarber.avant.dto.AgendamentoDTO;
import com.avantbarber.avant.dto.AgendamentoRequestDTO;
import com.avantbarber.avant.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @GetMapping
    public ResponseEntity<List<AgendamentoDTO>> buscarTodos() {
        return ResponseEntity.ok(agendamentoService.listarAgendamentos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id));
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<LocalTime>> listarHorariosDisponiveis(@RequestParam Long barbeiroId, @RequestParam LocalDate data) {
        return ResponseEntity.ok(agendamentoService.listarHorariosDisponiveis(barbeiroId, data));
    }

    @PostMapping
    public ResponseEntity<AgendamentoDTO> salvar(@Valid @RequestBody AgendamentoRequestDTO agendamentoRequestDTO) {
        return ResponseEntity.status(201).body(agendamentoService.salvar(agendamentoRequestDTO));
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<AgendamentoDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.confirmar(id));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoDTO> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.cancelar(id));
    }

    @PutMapping("/{id}/reagendar")
    public ResponseEntity<AgendamentoDTO> reagendar(@PathVariable Long id, @Valid @RequestParam LocalDateTime novaData) {
        return ResponseEntity.ok(agendamentoService.reagendar(id, novaData));
    }
}
