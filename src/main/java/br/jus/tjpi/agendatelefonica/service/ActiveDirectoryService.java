package br.jus.tjpi.agendatelefonica.service;

import br.jus.tjpi.agendatelefonica.config.AdProperties;
import br.jus.tjpi.agendatelefonica.dto.AdUserInfo;
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

    private final AdProperties adProperties;

    public ActiveDirectoryService(AdProperties adProperties) {
        this.adProperties = adProperties;
    }

    public boolean autenticarAd(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        for (String principal : buildPrincipals(username)) {
            if (tryBind(principal, password)) {
                return true;
            }
        }

        return false;
    }

    public Optional<AdUserInfo> validarEObterUsuarioAd(String username, String password) {
        if (!autenticarAd(username, password)) {
            return Optional.empty();
        }
        return buscarUsuarioPorUsername(username);
    }

    public Optional<AdUserInfo> buscarUsuarioPorUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        LdapTemplate ldapTemplate = createServiceLdapTemplate();
        LdapQuery query = LdapQueryBuilder.query()
            .base(adProperties.getBaseDn())
            .searchScope(SearchScope.SUBTREE)
            .attributes("displayName", "sAMAccountName", "userPrincipalName")
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
        return new AdUserInfo(displayName, samAccountName, userPrincipalName);
    }

    private String readAttribute(Attributes attributes, String name) throws NamingException {
        if (attributes.get(name) == null) {
            return null;
        }
        Object value = attributes.get(name).get();
        return value != null ? value.toString() : null;
    }

    private LdapTemplate createServiceLdapTemplate() {
        if (adProperties.getBindDn() == null || adProperties.getBindDn().isBlank()
            || adProperties.getBindPassword() == null || adProperties.getBindPassword().isBlank()) {
            throw new IllegalStateException("AD bind account não configurada. Defina AD_BIND_DN e AD_BIND_PASSWORD.");
        }

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(adProperties.getServerUrl());
        contextSource.setUserDn(adProperties.getBindDn());
        contextSource.setPassword(adProperties.getBindPassword());
        contextSource.afterPropertiesSet();

        return new LdapTemplate(contextSource);
    }

    private boolean tryBind(String principal, String password) {
        try {
            LdapContextSource contextSource = new LdapContextSource();
            contextSource.setUrl(adProperties.getServerUrl());
            contextSource.setUserDn(principal);
            contextSource.setPassword(password);
            contextSource.afterPropertiesSet();
            contextSource.getReadOnlyContext().close();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<String> buildPrincipals(String username) {
        String normalized = username.trim();
        List<String> principals = new ArrayList<>();

        if (normalized.contains("@") || normalized.contains("\\")) {
            principals.add(normalized);
            return principals;
        }

        if (adProperties.getDomain() != null && !adProperties.getDomain().isBlank()) {
            principals.add(adProperties.getDomain() + "\\" + normalized);
        }

        if (adProperties.getUpnSuffix() != null && !adProperties.getUpnSuffix().isBlank()) {
            principals.add(normalized + "@" + adProperties.getUpnSuffix());
        }

        principals.add(normalized);
        return principals;
    }
}
