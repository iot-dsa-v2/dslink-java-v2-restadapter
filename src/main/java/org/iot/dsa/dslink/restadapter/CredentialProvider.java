package org.iot.dsa.dslink.restadapter;

public interface CredentialProvider {
    
    public String getUsername();

    public String getPassword();
    
    public String getClientId();

    public String getClientSecret();

    public String getTokenURL();
    
    public Util.AUTH_SCHEME getAuthScheme();

    public String getToken();

}
