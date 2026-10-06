package com.avantbarber.avant.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80, nullable = false)
    private String nome;

    @Column(length = 14, unique = true)
    private String cpf;

    @Column(length = 20, nullable = false, unique = true)
    private String numero;

    @Column(length = 255)
    private String senha;

    @Column(length = 100)
    private String endereco;
    

}
