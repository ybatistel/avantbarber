package com.avantbarber.avant.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.avantbarber.avant.dto.ClienteDTO;
import com.avantbarber.avant.dto.ClienteRequestDTO;
import com.avantbarber.avant.exception.ChaveDuplicadaException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.Cliente;
import com.avantbarber.avant.repository.ClienteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public List<ClienteDTO> listarClientes() {
        return clienteRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public ClienteDTO buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + id));
    }

    public ClienteDTO salvar(ClienteRequestDTO clienteRequestDTO) {
        Cliente cliente = toEntity(clienteRequestDTO);
        if (cliente.getCpf() != null && clienteRepository.findByCpf(cliente.getCpf()).isPresent()) {
            throw new ChaveDuplicadaException("Erro: Já existe um cliente com o CPF informado!");
        }
        Cliente savedCliente = clienteRepository.save(cliente);
        return toDTO(savedCliente);
    }

    public ClienteDTO atualizar(Long id, ClienteRequestDTO clienteRequestDTO) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + id));
        cliente.setNome(clienteRequestDTO.getNome());
        cliente.setNumero(clienteRequestDTO.getNumero());
        cliente.setCpf(clienteRequestDTO.getCpf());
        cliente.setSenha(encodeSenha(clienteRequestDTO.getSenha()));
        cliente.setEndereco(clienteRequestDTO.getEndereco());
        Cliente updatedCliente = clienteRepository.save(cliente);
        return toDTO(updatedCliente);
    }

    public void deletar(Long id) {
        clienteRepository.deleteById(id);
    }

    private ClienteDTO toDTO(Cliente cliente) {
        return new ClienteDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getNumero(),
                cliente.getCpf(),
                cliente.getEndereco());
    }

    private Cliente toEntity(ClienteRequestDTO clienteRequestDTO) {
        Cliente cliente = new Cliente();
        cliente.setNome(clienteRequestDTO.getNome());
        cliente.setNumero(clienteRequestDTO.getNumero());
        cliente.setCpf(clienteRequestDTO.getCpf());
        cliente.setSenha(encodeSenha(clienteRequestDTO.getSenha()));
        cliente.setEndereco(clienteRequestDTO.getEndereco());
        return cliente;
    }

    private String encodeSenha(String senha) {
        return senha == null ? null : passwordEncoder.encode(senha);
    }
}
