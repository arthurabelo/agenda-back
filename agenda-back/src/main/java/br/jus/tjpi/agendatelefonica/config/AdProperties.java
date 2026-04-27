package br.jus.tjpi.agendatelefonica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ad")
public class AdProperties {

    private String serverUrl;
    private String domain;
    private String upnSuffix;
    private String baseDn;
    private String loginGroup;

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

    public String getLoginGroup() {
        return loginGroup;
    }

    public void setLoginGroup(String loginGroup) {
        this.loginGroup = loginGroup;
    }
}
