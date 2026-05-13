package com.avantbarber.avant.dto;

import com.avantbarber.avant.model.Perfil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BarbeiroRequestDTO {
    private Long id;
    private String nome;
    private String numero;
    private String cpf;
    private String senha;
    private Perfil perfil;
}
