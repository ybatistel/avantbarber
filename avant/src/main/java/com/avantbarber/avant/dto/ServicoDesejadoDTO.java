package com.avantbarber.avant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicoDesejadoDTO {

    private Long id;
    private String nome;
    private double preco;

    
}
