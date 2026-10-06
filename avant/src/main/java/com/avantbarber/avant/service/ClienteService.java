package com.avantbarber.avant.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.avantbarber.avant.dto.ClienteDTO;
import com.avantbarber.avant.dto.ClienteRequestDTO;
import com.avantbarber.avant.exception.BusinessException;
import com.avantbarber.avant.exception.ChaveDuplicadaException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.Cliente;
import com.avantbarber.avant.repository.ClienteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final int TAMANHO_MAXIMO_NUMERO = 20;
    private static final String MENSAGEM_NUMERO_DUPLICADO = "Erro: Já existe um cliente com o número informado!";

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

    public ClienteDTO buscarPorNumero(String numero) {
        String numeroNormalizado = normalizarNumero(numero);
        validarNumero(numeroNormalizado);
        return clienteRepository.findByNumero(numeroNormalizado)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o número: " + numeroNormalizado));
    }

    public ClienteDTO salvar(ClienteRequestDTO clienteRequestDTO) {
        Cliente cliente = toEntity(clienteRequestDTO);
        validarNumero(cliente.getNumero());
        if (cliente.getCpf() != null && clienteRepository.findByCpf(cliente.getCpf()).isPresent()) {
            throw new ChaveDuplicadaException("Erro: Já existe um cliente com o CPF informado!");
        }
        validarNumeroUnico(cliente.getNumero(), null);
        try {
            return toDTO(clienteRepository.save(cliente));
        } catch (DataIntegrityViolationException e) {
            throw traduzirViolacaoDeUnicidade(cliente, null, e);
        }
    }

    public ClienteDTO atualizar(Long id, ClienteRequestDTO clienteRequestDTO) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com o ID: " + id));
        String numeroNormalizado = normalizarNumero(clienteRequestDTO.getNumero());
        validarNumero(numeroNormalizado);
        validarNumeroUnico(numeroNormalizado, id);
        cliente.setNome(clienteRequestDTO.getNome());
        cliente.setNumero(numeroNormalizado);
        cliente.setCpf(clienteRequestDTO.getCpf());
        cliente.setSenha(encodeSenha(clienteRequestDTO.getSenha()));
        cliente.setEndereco(clienteRequestDTO.getEndereco());
        try {
            return toDTO(clienteRepository.save(cliente));
        } catch (DataIntegrityViolationException e) {
            throw traduzirViolacaoDeUnicidade(cliente, id, e);
        }
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
        cliente.setNumero(normalizarNumero(clienteRequestDTO.getNumero()));
        cliente.setCpf(clienteRequestDTO.getCpf());
        cliente.setSenha(encodeSenha(clienteRequestDTO.getSenha()));
        cliente.setEndereco(clienteRequestDTO.getEndereco());
        return cliente;
    }

    // O número de WhatsApp identifica o cliente: guarda só os dígitos para que
    // "+55 (11) 99999-9999" e "5511999999999" sejam o mesmo cliente.
    private String normalizarNumero(String numero) {
        return numero == null ? null : numero.replaceAll("\\D", "");
    }

    // Número já normalizado: precisa ter ao menos um dígito e caber na coluna.
    private void validarNumero(String numeroNormalizado) {
        if (numeroNormalizado == null || numeroNormalizado.isEmpty()
                || numeroNormalizado.length() > TAMANHO_MAXIMO_NUMERO) {
            throw new BusinessException("Erro: O número informado é inválido!");
        }
    }

    private void validarNumeroUnico(String numeroNormalizado, Long clienteIdAtual) {
        if (existeOutroClienteComNumero(numeroNormalizado, clienteIdAtual)) {
            throw new ChaveDuplicadaException(MENSAGEM_NUMERO_DUPLICADO);
        }
    }

    private boolean existeOutroClienteComNumero(String numeroNormalizado, Long clienteIdAtual) {
        return clienteRepository.findByNumero(numeroNormalizado)
                .filter(existente -> !existente.getId().equals(clienteIdAtual))
                .isPresent();
    }

    // A checagem prévia de unicidade não é atômica: duas requisições simultâneas (ex: duas
    // mensagens seguidas no WhatsApp) podem passar nela e a constraint do banco barra a
    // segunda. Só vira 409 se de fato outro cliente tem o número/CPF — qualquer outra
    // violação de integridade é relançada como veio, sem ser mascarada.
    private RuntimeException traduzirViolacaoDeUnicidade(Cliente cliente, Long clienteIdAtual, DataIntegrityViolationException original) {
        if (cliente.getNumero() != null && existeOutroClienteComNumero(cliente.getNumero(), clienteIdAtual)) {
            return new ChaveDuplicadaException(MENSAGEM_NUMERO_DUPLICADO);
        }
        if (cliente.getCpf() != null && clienteRepository.findByCpf(cliente.getCpf())
                .filter(existente -> !existente.getId().equals(clienteIdAtual))
                .isPresent()) {
            return new ChaveDuplicadaException("Erro: Já existe um cliente com o CPF informado!");
        }
        return original;
    }

    private String encodeSenha(String senha) {
        return senha == null ? null : passwordEncoder.encode(senha);
    }
}
