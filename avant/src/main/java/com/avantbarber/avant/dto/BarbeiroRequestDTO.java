package com.avantbarber.avant.dto;

import jakarta.validation.constraints.NotBlank;

import com.avantbarber.avant.model.Perfil;

import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "O nome é obrigatório")
    private String nome;
    @NotBlank(message = "O número é obrigatório")
    private String numero;
    @NotBlank(message = "O CPF é obrigatório")
    private String cpf;
    @NotBlank(message = "A senha é obrigatória")
    private String senha;
    @NotNull(message = "O perfil é obrigatório")
    private Perfil perfil;
}
