package br.jus.tjpi.agendatelefonica.dto;

public record LoginResponse(
        Long id,
        String username,
        String role,
        boolean active,
        String token
) {
}
