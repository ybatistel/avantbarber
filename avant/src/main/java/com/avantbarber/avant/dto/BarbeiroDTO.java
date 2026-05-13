package com.avantbarber.avant.dto;

import com.avantbarber.avant.model.Perfil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BarbeiroDTO {
    
    private Long id;
    private String nome;
    private String numero;
    private String cpf;
    private Perfil perfil;

}
