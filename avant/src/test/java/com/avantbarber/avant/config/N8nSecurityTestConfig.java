package com.avantbarber.avant.config;

import com.avantbarber.avant.controller.AgendamentoController;
import com.avantbarber.avant.controller.BarbeiroController;
import com.avantbarber.avant.controller.ClienteController;
import com.avantbarber.avant.controller.ServicoDesejadoController;
import com.avantbarber.avant.infra.RestExceptionHandler;
import com.avantbarber.avant.service.AgendamentoService;
import com.avantbarber.avant.service.BarbeiroService;
import com.avantbarber.avant.service.ClienteService;
import com.avantbarber.avant.service.ServicoDesejadoService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Contexto mínimo para testar a segurança de verdade (a {@link SpringConfig} real, com as duas
 * chains) sem a auto-configuração do Spring Boot e sem banco: controllers reais, services
 * mockados. Nenhum request chega a persistir nada.
 */
@Configuration
@EnableWebMvc
@EnableWebSecurity
@Import(SpringConfig.class)
class N8nSecurityTestConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId("id-de-teste").clientSecret("segredo-de-teste").build());
    }

    @Bean
    RestExceptionHandler restExceptionHandler() {
        return new RestExceptionHandler();
    }

    // Os services são beans (mocks) para que os testes possam verificar o que chega a eles.

    @Bean
    ClienteService clienteService() {
        return Mockito.mock(ClienteService.class);
    }

    @Bean
    AgendamentoService agendamentoService() {
        return Mockito.mock(AgendamentoService.class);
    }

    @Bean
    BarbeiroService barbeiroService() {
        return Mockito.mock(BarbeiroService.class);
    }

    @Bean
    ServicoDesejadoService servicoDesejadoService() {
        return Mockito.mock(ServicoDesejadoService.class);
    }

    @Bean
    ClienteController clienteController(ClienteService clienteService) {
        return new ClienteController(clienteService);
    }

    @Bean
    AgendamentoController agendamentoController(AgendamentoService agendamentoService) {
        return new AgendamentoController(agendamentoService);
    }

    @Bean
    BarbeiroController barbeiroController(BarbeiroService barbeiroService) {
        return new BarbeiroController(barbeiroService);
    }

    @Bean
    ServicoDesejadoController servicoDesejadoController(ServicoDesejadoService servicoDesejadoService) {
        return new ServicoDesejadoController(servicoDesejadoService);
    }
}
