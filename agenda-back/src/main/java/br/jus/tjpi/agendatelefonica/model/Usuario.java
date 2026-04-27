package br.jus.tjpi.agendatelefonica.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {
    @Id // Unique identifier for the entity
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates the ID value
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "ad_user", nullable = false, columnDefinition = "boolean default false")
    private boolean adUser = false;
}