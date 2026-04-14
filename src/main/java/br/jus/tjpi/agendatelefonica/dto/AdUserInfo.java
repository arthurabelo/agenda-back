package br.jus.tjpi.agendatelefonica.dto;

public record AdUserInfo(
        String displayName,
        String sAMAccountName,
        String userPrincipalName
) {
}
