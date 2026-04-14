package br.jus.tjpi.agendatelefonica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ad")
public class AdProperties {

    private String serverUrl = "ldap://SUECIA.tjpi.local:389";
    private String domain = "TJ-PI";
    private String upnSuffix = "tjpi.local";
    private String baseDn = "DC=tjpi,DC=local";
    private String bindDn;
    private String bindPassword;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUpnSuffix() {
        return upnSuffix;
    }

    public void setUpnSuffix(String upnSuffix) {
        this.upnSuffix = upnSuffix;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }
}
