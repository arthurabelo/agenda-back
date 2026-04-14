package br.jus.tjpi.agendatelefonica.dto;

public record AdRegisterResponse(
        Long id,
        String username,
        String displayName,
        String userPrincipalName,
        String role,
        boolean active
) {
}
