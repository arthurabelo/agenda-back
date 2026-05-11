package br.jus.tjpi.agendatelefonica.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Entity
@Table(name = "contatos")
@Data
public class Contato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    
    @Column(nullable = false)
    private String unidade;

    private String setor;

    @Column(nullable = false)
    private String comarca;

    private String endereco;
    
    private String localidade;
    
    private String telefone;

    @Column(name = "meio_de_contato")
    private String meioDeContato;

    @Column(name = "tipo_contato")
    private String tipoContato;
}
