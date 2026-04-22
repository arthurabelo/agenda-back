package br.jus.tjpi.agendatelefonica.dto;

import java.util.List;

public record AdUserInfo(
        String displayName,
        String sAMAccountName,
        String userPrincipalName,
        List<String> groups
) {
}
