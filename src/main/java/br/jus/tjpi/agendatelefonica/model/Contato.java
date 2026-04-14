package br.jus.tjpi.agendatelefonica.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Entity
@Table(name = "contatos")
@Data
public class Contato {
    @Id // Unique identifier for the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates the ID value
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    
    @Column(nullable = false) // Specifies that the column cannot be null
    private String setor;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String local;
}
