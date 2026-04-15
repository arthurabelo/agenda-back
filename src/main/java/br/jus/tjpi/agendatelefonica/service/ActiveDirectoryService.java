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

    public Optional<AdUserInfo> autenticarEObterUsuarioAd(String username, String password) {
        if (!autenticarAd(username, password)) {
            return Optional.empty();
        }
        return buscarUsuarioPorUsernameComCredenciais(username, password);
    }

    public Optional<AdUserInfo> buscarUsuarioPorUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        if (!hasBindAccount()) {
            throw new IllegalStateException("Cadastro AD requer AD_BIND_DN e AD_BIND_PASSWORD para verificar a existência do usuário no AD.");
        }

        return buscarComServiceAccount(username);
    }

    public Optional<AdUserInfo> buscarUsuarioPorUsernameComCredenciais(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        for (String principal : buildPrincipals(username)) {
            try {
                LdapTemplate ldapTemplate = createAuthenticatedLdapTemplate(principal, password);
                return searchUserByUsername(ldapTemplate, username);
            } catch (Exception e) {
                logger.debug("Falha ao buscar usuário com principal {}: {}", principal, e.getMessage());
            }
        }

        return Optional.empty();
    }

    public boolean usuarioPertenceAoGrupo(String username, String groupName, String password) {
        Optional<AdUserInfo> adUserInfo = autenticarEObterUsuarioAd(username, password);
        if (adUserInfo.isEmpty()) {
            return false;
        }
        return adUserInfo.get().groups().stream()
                .anyMatch(group -> group.equalsIgnoreCase(groupName));
    }

    private boolean hasBindAccount() {
        return adProperties.getBindDn() != null && !adProperties.getBindDn().isBlank()
            && adProperties.getBindPassword() != null && !adProperties.getBindPassword().isBlank();
    }

    private Optional<AdUserInfo> buscarComServiceAccount(String username) {
        try {
            return searchUserByUsername(createServiceLdapTemplate(), username);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private LdapTemplate createAuthenticatedLdapTemplate(String principal, String password) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(adProperties.getServerUrl());
        contextSource.setUserDn(principal);
        contextSource.setPassword(password);
        contextSource.afterPropertiesSet();
        return new LdapTemplate(contextSource);
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

    private LdapTemplate createServiceLdapTemplate() {
        if (!hasBindAccount()) {
            throw new IllegalStateException("AD_BIND_DN e AD_BIND_PASSWORD são obrigatórios para consultar o AD sem credenciais do usuário.");
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

        // Se já tem formato de principal (contém @ ou \), adiciona direto
        if (normalized.contains("@") || normalized.contains("\\")) {
            principals.add(normalized);
            return principals;
        }

        // Formato NetBIOS: TJ-PI\rabeloarthur
        if (adProperties.getDomain() != null && !adProperties.getDomain().isBlank()) {
            principals.add(adProperties.getDomain() + "\\" + normalized);
            logger.debug("Adicionado principal NetBIOS: {}", principals.get(principals.size() - 1));
        }

        // Formato UPN: rabeloarthur@tjpi.local
        if (adProperties.getUpnSuffix() != null && !adProperties.getUpnSuffix().isBlank()) {
            principals.add(normalized + "@" + adProperties.getUpnSuffix());
            logger.debug("Adicionado principal UPN: {}", principals.get(principals.size() - 1));
        }

        // Formato SAM: rabeloarthur (último recurso)
        principals.add(normalized);
        logger.debug("Adicionado principal SAM: {}", normalized);

        return principals;
    }
}
