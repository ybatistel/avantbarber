package com.avantbarber.avant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequestDTO {
    private Long id;
    private String nome;
    private String numero;
    private String cpf;
    private String senha;
    private String endereco;
}
