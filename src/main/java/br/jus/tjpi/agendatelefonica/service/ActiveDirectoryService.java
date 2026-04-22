package br.jus.tjpi.agendatelefonica.service;

import br.jus.tjpi.agendatelefonica.config.AdProperties;
import br.jus.tjpi.agendatelefonica.dto.AdUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.query.SearchScope;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ActiveDirectoryService {

    private static final Logger logger = LoggerFactory.getLogger(ActiveDirectoryService.class);
    private final AdProperties adProperties;

    public ActiveDirectoryService(AdProperties adProperties) {
        this.adProperties = adProperties;
    }

    public Optional<AdUserInfo> autenticarEObterUsuarioAd(String username, String password) {
        return buscarUsuarioPorUsernameComCredenciais(username, password);
    }

    public Optional<AdUserInfo> buscarUsuarioPorUsernameComCredenciais(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        for (String principal : buildPrincipals(username)) {
            try {
                LdapTemplate ldapTemplate = createAuthenticatedLdapTemplate(principal, password);
                Optional<AdUserInfo> user = searchUserByUsername(ldapTemplate, username);
                if (user.isPresent()) {
                    return user;
                }
            } catch (Exception e) {
                logger.debug("Falha ao buscar usuário no AD com um dos principais suportados: {}", e.getMessage());
            }
        }

        return Optional.empty();
    }

    public boolean usuarioPertenceAoGrupo(AdUserInfo adUserInfo, String groupName) {
        if (adUserInfo == null || groupName == null || groupName.isBlank()) {
            return false;
        }

        String normalizedGroupName = normalizeGroupName(groupName);
        return adUserInfo.groups().stream()
                .anyMatch(group -> group.equalsIgnoreCase(normalizedGroupName));
    }

    private LdapTemplate createAuthenticatedLdapTemplate(String principal, String password) {
        LdapContextSource contextSource = createContextSource(principal, password);
        contextSource.afterPropertiesSet();
        LdapTemplate ldapTemplate = new LdapTemplate(contextSource);
        // AD frequentemente retorna referrals; ignorar partial result evita falhas indevidas no search.
        ldapTemplate.setIgnorePartialResultException(true);
        return ldapTemplate;
    }

    private Optional<AdUserInfo> searchUserByUsername(LdapTemplate ldapTemplate, String username) {
        LdapQuery query = LdapQueryBuilder.query()
            .base(adProperties.getBaseDn())
            .searchScope(SearchScope.SUBTREE)
            .attributes("displayName", "sAMAccountName", "userPrincipalName", "memberOf")
            .where("sAMAccountName").is(username.trim());

        List<AdUserInfo> results = ldapTemplate.search(query, (AttributesMapper<AdUserInfo>) this::mapAdUser);

        if (results == null || results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.get(0));
    }

    private AdUserInfo mapAdUser(Attributes attributes) throws NamingException {
        String displayName = readAttribute(attributes, "displayName");
        String samAccountName = readAttribute(attributes, "sAMAccountName");
        String userPrincipalName = readAttribute(attributes, "userPrincipalName");
        List<String> groups = extractGroupsFromMemberOf(attributes);
        return new AdUserInfo(displayName, samAccountName, userPrincipalName, groups);
    }

    private List<String> extractGroupsFromMemberOf(Attributes attributes) throws NamingException {
        List<String> groups = new ArrayList<>();
        if (attributes.get("memberOf") == null) {
            return groups;
        }

        javax.naming.directory.Attribute memberOfAttr = attributes.get("memberOf");
        if (memberOfAttr != null) {
            javax.naming.NamingEnumeration<?> values = memberOfAttr.getAll();
            while (values.hasMore()) {
                String dn = (String) values.next();
                // Extrai o CN do DN
                String cn = extractCNFromDN(dn);
                if (cn != null) {
                    groups.add(cn);
                }
            }
        }
        return groups;
    }

    private String extractCNFromDN(String dn) {
        // Extrai o CN de um DN como "CN=G.stic.agendatelefonica.superadmin,OU=Groups,DC=tjpi,DC=local"
        if (dn == null || dn.isBlank()) {
            return null;
        }
        int cnIndex = dn.indexOf("CN=");
        if (cnIndex == -1) {
            return null;
        }
        int endIndex = dn.indexOf(",", cnIndex);
        if (endIndex == -1) {
            return dn.substring(cnIndex + 3);
        }
        return dn.substring(cnIndex + 3, endIndex);
    }

    private String readAttribute(Attributes attributes, String name) throws NamingException {
        if (attributes.get(name) == null) {
            return null;
        }
        Object value = attributes.get(name).get();
        return value != null ? value.toString() : null;
    }

    private LdapContextSource createContextSource(String principal, String password) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(adProperties.getServerUrl());
        contextSource.setUserDn(principal);
        contextSource.setPassword(password);
        contextSource.setReferral("ignore");
        return contextSource;
    }

    private String normalizeGroupName(String groupName) {
        String trimmedGroup = groupName.trim();
        if (trimmedGroup.startsWith("CN=")) {
            String cn = extractCNFromDN(trimmedGroup);
            return cn != null ? cn : trimmedGroup;
        }
        return trimmedGroup;
    }

    private List<String> buildPrincipals(String username) {
        String normalized = username.trim();
        List<String> principals = new ArrayList<>();

        // Se já tem formato de principal (contém @ ou \), adiciona direto
        if (normalized.contains("@") || normalized.contains("\\")) {
            principals.add(normalized);
            return principals;
        }

        // Formato NetBIOS: DOMINIO\\usuario
        if (adProperties.getDomain() != null && !adProperties.getDomain().isBlank()) {
            principals.add(adProperties.getDomain() + "\\" + normalized);
        }

        // Formato UPN: usuario@dominio.local
        if (adProperties.getUpnSuffix() != null && !adProperties.getUpnSuffix().isBlank()) {
            principals.add(normalized + "@" + adProperties.getUpnSuffix());
        }

        return principals;
    }
}
