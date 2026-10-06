package com.avantbarber.avant.service;

import com.avantbarber.avant.dto.ClienteDTO;
import com.avantbarber.avant.dto.ClienteRequestDTO;
import com.avantbarber.avant.exception.BusinessException;
import com.avantbarber.avant.exception.ChaveDuplicadaException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.Cliente;
import com.avantbarber.avant.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClienteService clienteService;

    private static final Long CLIENTE_ID = 1L;
    private static final Long OUTRO_CLIENTE_ID = 2L;
    private static final String NUMERO_NORMALIZADO = "5511999999999";

    private static Cliente cliente(Long id, String numero) {
        return Cliente.builder().id(id).nome("Cliente Teste").numero(numero).build();
    }

    private static ClienteRequestDTO request(String numero) {
        return new ClienteRequestDTO(null, "Cliente Teste", numero, null, null, null);
    }

    // ---------- buscarPorNumero ----------

    @Test
    void buscarPorNumero_deveRetornarCliente_quandoNumeroExiste() {
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.of(cliente(CLIENTE_ID, NUMERO_NORMALIZADO)));

        ClienteDTO resultado = clienteService.buscarPorNumero(NUMERO_NORMALIZADO);

        assertThat(resultado.getId()).isEqualTo(CLIENTE_ID);
        assertThat(resultado.getNumero()).isEqualTo(NUMERO_NORMALIZADO);
    }

    @Test
    void buscarPorNumero_deveNormalizarParaSoDigitosAntesDeConsultar() {
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.of(cliente(CLIENTE_ID, NUMERO_NORMALIZADO)));

        ClienteDTO resultado = clienteService.buscarPorNumero("+55 (11) 99999-9999");

        assertThat(resultado.getId()).isEqualTo(CLIENTE_ID);
        verify(clienteRepository).findByNumero(NUMERO_NORMALIZADO);
    }

    @Test
    void buscarPorNumero_deveLancarRecursoNaoEncontradoException_quandoNumeroNaoExiste() {
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorNumero("+55 11 99999-9999"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "+-() "})
    void buscarPorNumero_deveLancarBusinessException_quandoNumeroNaoTemDigitos(String numero) {
        assertThatThrownBy(() -> clienteService.buscarPorNumero(numero))
                .isInstanceOf(BusinessException.class);

        verify(clienteRepository, never()).findByNumero(anyString());
    }

    @Test
    void buscarPorNumero_deveLancarBusinessException_quandoNumeroNulo() {
        assertThatThrownBy(() -> clienteService.buscarPorNumero(null))
                .isInstanceOf(BusinessException.class);

        verify(clienteRepository, never()).findByNumero(anyString());
    }

    // ---------- salvar ----------

    @Test
    void salvar_deveGravarNumeroSoComDigitos() {
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteDTO resultado = clienteService.salvar(request("+55 (11) 99999-9999"));

        ArgumentCaptor<Cliente> salvo = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(salvo.capture());
        assertThat(salvo.getValue().getNumero()).isEqualTo(NUMERO_NORMALIZADO);
        assertThat(resultado.getNumero()).isEqualTo(NUMERO_NORMALIZADO);
    }

    @Test
    void salvar_deveLancarChaveDuplicadaException_quandoOutroClienteJaTemOMesmoNumero() {
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.of(cliente(OUTRO_CLIENTE_ID, NUMERO_NORMALIZADO)));

        assertThatThrownBy(() -> clienteService.salvar(request("+55 11 99999-9999")))
                .isInstanceOf(ChaveDuplicadaException.class);

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void salvar_deveAceitarClienteSemCpfSenhaEEndereco() {
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteDTO resultado = clienteService.salvar(request(NUMERO_NORMALIZADO));

        assertThat(resultado.getCpf()).isNull();
        assertThat(resultado.getEndereco()).isNull();
        verify(clienteRepository, never()).findByCpf(anyString());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void salvar_deveAplicarHashNaSenha_quandoInformada() {
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("segredo")).thenReturn("hash-bcrypt");
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        clienteService.salvar(new ClienteRequestDTO(null, "Cliente Teste", NUMERO_NORMALIZADO, null, "segredo", null));

        ArgumentCaptor<Cliente> salvo = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(salvo.capture());
        assertThat(salvo.getValue().getSenha()).isEqualTo("hash-bcrypt");
    }

    @Test
    void salvar_deveLancarChaveDuplicadaException_quandoCpfJaExiste() {
        when(clienteRepository.findByCpf("12345678900")).thenReturn(Optional.of(cliente(OUTRO_CLIENTE_ID, "5511888888888")));

        assertThatThrownBy(() -> clienteService.salvar(
                new ClienteRequestDTO(null, "Cliente Teste", NUMERO_NORMALIZADO, "12345678900", null, null)))
                .isInstanceOf(ChaveDuplicadaException.class);

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    // ---------- atualizar ----------

    @Test
    void atualizar_deveLancarRecursoNaoEncontradoException_quandoClienteNaoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.atualizar(99L, request(NUMERO_NORMALIZADO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizar_deveNormalizarONumero() {
        Cliente existente = cliente(CLIENTE_ID, "5511000000000");
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteDTO resultado = clienteService.atualizar(CLIENTE_ID, request("+55 (11) 99999-9999"));

        assertThat(resultado.getNumero()).isEqualTo(NUMERO_NORMALIZADO);
        assertThat(existente.getNumero()).isEqualTo(NUMERO_NORMALIZADO);
    }

    @Test
    void atualizar_deveLancarChaveDuplicadaException_quandoOutroClienteJaTemOMesmoNumero() {
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, "5511000000000")));
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.of(cliente(OUTRO_CLIENTE_ID, NUMERO_NORMALIZADO)));

        assertThatThrownBy(() -> clienteService.atualizar(CLIENTE_ID, request(NUMERO_NORMALIZADO)))
                .isInstanceOf(ChaveDuplicadaException.class);

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void atualizar_naoDeveLancarExcecao_quandoONumeroJaEDoProprioCliente() {
        Cliente existente = cliente(CLIENTE_ID, NUMERO_NORMALIZADO);
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteDTO resultado = clienteService.atualizar(CLIENTE_ID, request("+55 (11) 99999-9999"));

        assertThat(resultado.getId()).isEqualTo(CLIENTE_ID);
        verify(clienteRepository).save(existente);
    }

    // ---------- validade do número (salvar / atualizar / buscar) ----------

    private static final String VINTE_DIGITOS = "12345678901234567890";
    private static final String VINTE_E_UM_DIGITOS = "123456789012345678901";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "---", "+-() ", VINTE_E_UM_DIGITOS, "+55 (11) 91234-5678 / 9999-99999-99"})
    void salvar_deveLancarBusinessException_quandoONumeroNaoEValido(String numero) {
        assertThatThrownBy(() -> clienteService.salvar(request(numero)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("número informado é inválido");

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "---", VINTE_E_UM_DIGITOS})
    void atualizar_deveLancarBusinessException_quandoONumeroNaoEValido(String numero) {
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, NUMERO_NORMALIZADO)));

        assertThatThrownBy(() -> clienteService.atualizar(CLIENTE_ID, request(numero)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("número informado é inválido");

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void salvar_deveAceitarNumeroComExatamenteVinteDigitos() {
        when(clienteRepository.findByNumero(VINTE_DIGITOS)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteDTO resultado = clienteService.salvar(request(VINTE_DIGITOS));

        assertThat(resultado.getNumero()).isEqualTo(VINTE_DIGITOS);
    }

    @Test
    void atualizar_deveAceitarNumeroComExatamenteVinteDigitos() {
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, NUMERO_NORMALIZADO)));
        when(clienteRepository.findByNumero(VINTE_DIGITOS)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        ClienteDTO resultado = clienteService.atualizar(CLIENTE_ID, request(VINTE_DIGITOS));

        assertThat(resultado.getNumero()).isEqualTo(VINTE_DIGITOS);
    }

    @Test
    void buscarPorNumero_deveLancarBusinessException_quandoTemMaisDeVinteDigitos() {
        assertThatThrownBy(() -> clienteService.buscarPorNumero(VINTE_E_UM_DIGITOS))
                .isInstanceOf(BusinessException.class);

        verify(clienteRepository, never()).findByNumero(anyString());
    }

    // ---------- corrida: a constraint do banco barra o que a checagem prévia deixou passar ----------

    @Test
    void salvar_deveTraduzirViolacaoDeIntegridadeParaChaveDuplicada_quandoOutroClienteJaTemONumero() {
        // 1ª consulta (checagem prévia): livre; 2ª (após a exceção): outro cliente acabou de gravar.
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO))
                .thenReturn(Optional.empty(), Optional.of(cliente(OUTRO_CLIENTE_ID, NUMERO_NORMALIZADO)));
        when(clienteRepository.save(any(Cliente.class))).thenThrow(new DataIntegrityViolationException("uk_numero"));

        assertThatThrownBy(() -> clienteService.salvar(request(NUMERO_NORMALIZADO)))
                .isInstanceOf(ChaveDuplicadaException.class)
                .hasMessageContaining("número");
    }

    @Test
    void salvar_deveTraduzirViolacaoDeIntegridadeParaChaveDuplicada_quandoOutroClienteJaTemOCpf() {
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.empty());
        when(clienteRepository.findByCpf("12345678900"))
                .thenReturn(Optional.empty(), Optional.of(cliente(OUTRO_CLIENTE_ID, "5511888888888")));
        when(clienteRepository.save(any(Cliente.class))).thenThrow(new DataIntegrityViolationException("uk_cpf"));

        assertThatThrownBy(() -> clienteService.salvar(
                new ClienteRequestDTO(null, "Cliente Teste", NUMERO_NORMALIZADO, "12345678900", null, null)))
                .isInstanceOf(ChaveDuplicadaException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void salvar_deveRelancarAViolacaoOriginal_quandoNenhumOutroClienteTemONumeroOuOCpf() {
        DataIntegrityViolationException original = new DataIntegrityViolationException("nome nulo");
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenThrow(original);

        assertThatThrownBy(() -> clienteService.salvar(request(NUMERO_NORMALIZADO)))
                .isSameAs(original);
    }

    @Test
    void atualizar_deveTraduzirViolacaoDeIntegridadeParaChaveDuplicada_quandoOutroClienteJaTemONumero() {
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(cliente(CLIENTE_ID, "5511000000000")));
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO))
                .thenReturn(Optional.empty(), Optional.of(cliente(OUTRO_CLIENTE_ID, NUMERO_NORMALIZADO)));
        when(clienteRepository.save(any(Cliente.class))).thenThrow(new DataIntegrityViolationException("uk_numero"));

        assertThatThrownBy(() -> clienteService.atualizar(CLIENTE_ID, request(NUMERO_NORMALIZADO)))
                .isInstanceOf(ChaveDuplicadaException.class)
                .hasMessageContaining("número");
    }

    @Test
    void atualizar_deveRelancarAViolacaoOriginal_quandoOUnicoClienteComONumeroEOProprio() {
        Cliente existente = cliente(CLIENTE_ID, NUMERO_NORMALIZADO);
        DataIntegrityViolationException original = new DataIntegrityViolationException("outra violação");
        when(clienteRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(existente));
        when(clienteRepository.findByNumero(NUMERO_NORMALIZADO)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenThrow(original);

        assertThatThrownBy(() -> clienteService.atualizar(CLIENTE_ID, request(NUMERO_NORMALIZADO)))
                .isSameAs(original);
    }
}
