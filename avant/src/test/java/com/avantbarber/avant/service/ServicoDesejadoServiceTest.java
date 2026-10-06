package com.avantbarber.avant.service;

import com.avantbarber.avant.dto.ServicoDesejadoDTO;
import com.avantbarber.avant.exception.BusinessException;
import com.avantbarber.avant.exception.RecursoNaoEncontradoException;
import com.avantbarber.avant.model.ServicoDesejado;
import com.avantbarber.avant.repository.ServicoDesejadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoDesejadoServiceTest {

    @Mock
    private ServicoDesejadoRepository servicoDesejadoRepository;

    @InjectMocks
    private ServicoDesejadoService servicoDesejadoService;

    private static final Long SERVICO_ID = 1L;

    private static ServicoDesejado servico(Long id, int duracaoMinutos) {
        return new ServicoDesejado(id, "Corte", BigDecimal.valueOf(50), duracaoMinutos);
    }

    private static ServicoDesejadoDTO dto(int duracaoMinutos) {
        return new ServicoDesejadoDTO(null, "Corte", BigDecimal.valueOf(50), duracaoMinutos);
    }

    // ---------- listar / buscarPorId ----------

    @Test
    void listarServicosDesejados_deveRetornarTodosMapeadosParaDTO() {
        when(servicoDesejadoRepository.findAll()).thenReturn(List.of(servico(SERVICO_ID, 30)));

        List<ServicoDesejadoDTO> resultado = servicoDesejadoService.listarServicosDesejados();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getDuracaoMinutos()).isEqualTo(30);
    }

    @Test
    void buscarPorId_deveRetornarDTO_quandoServicoExiste() {
        when(servicoDesejadoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico(SERVICO_ID, 60)));

        ServicoDesejadoDTO resultado = servicoDesejadoService.buscarPorId(SERVICO_ID);

        assertThat(resultado.getId()).isEqualTo(SERVICO_ID);
        assertThat(resultado.getDuracaoMinutos()).isEqualTo(60);
    }

    @Test
    void buscarPorId_deveLancarRecursoNaoEncontradoException_quandoIdNaoExiste() {
        when(servicoDesejadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoDesejadoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- salvar ----------

    @Test
    void salvar_deveCriarServico_quandoDuracaoValida() {
        when(servicoDesejadoRepository.save(any(ServicoDesejado.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ServicoDesejadoDTO resultado = servicoDesejadoService.salvar(dto(30));

        assertThat(resultado.getDuracaoMinutos()).isEqualTo(30);
        verify(servicoDesejadoRepository).save(any(ServicoDesejado.class));
    }

    @ParameterizedTest(name = "duração {0} deve ser rejeitada")
    @ValueSource(ints = {0, -30, 15, 45})
    void salvar_deveLancarBusinessException_quandoDuracaoInvalida(int duracaoInvalida) {
        assertThatThrownBy(() -> servicoDesejadoService.salvar(dto(duracaoInvalida)))
                .isInstanceOf(BusinessException.class);

        verify(servicoDesejadoRepository, never()).save(any());
    }

    @Test
    void salvar_deveLancarBusinessException_quandoDuracaoNula() {
        ServicoDesejadoDTO semDuracao = new ServicoDesejadoDTO(null, "Corte", BigDecimal.valueOf(50), null);

        assertThatThrownBy(() -> servicoDesejadoService.salvar(semDuracao))
                .isInstanceOf(BusinessException.class);

        verify(servicoDesejadoRepository, never()).save(any());
    }

    // ---------- atualizar ----------

    @Test
    void atualizar_deveAlterarDadosDoServico_quandoDuracaoValida() {
        when(servicoDesejadoRepository.findById(SERVICO_ID)).thenReturn(Optional.of(servico(SERVICO_ID, 30)));
        when(servicoDesejadoRepository.save(any(ServicoDesejado.class))).thenAnswer(inv -> inv.getArgument(0));

        ServicoDesejadoDTO resultado = servicoDesejadoService.atualizar(SERVICO_ID, dto(90));

        assertThat(resultado.getDuracaoMinutos()).isEqualTo(90);
    }

    @Test
    void atualizar_deveLancarRecursoNaoEncontradoException_quandoIdNaoExiste() {
        when(servicoDesejadoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicoDesejadoService.atualizar(99L, dto(30)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void atualizar_deveLancarBusinessException_quandoDuracaoInvalida() {
        assertThatThrownBy(() -> servicoDesejadoService.atualizar(SERVICO_ID, dto(45)))
                .isInstanceOf(BusinessException.class);

        verify(servicoDesejadoRepository, never()).save(any());
    }

    // ---------- deletar ----------

    @Test
    void deletar_deveChamarRepository() {
        servicoDesejadoService.deletar(SERVICO_ID);

        verify(servicoDesejadoRepository).deleteById(SERVICO_ID);
    }
}
