package org.iot.dsa.dslink.restadapter;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.Consumer;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.clientcred.ClientCredentialsGrant;
import org.apache.cxf.rs.security.oauth2.grants.owner.ResourceOwnerGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.iot.dsa.logging.DSLogger;
import org.iot.dsa.util.DSException;

import java.util.concurrent.TimeUnit;

/**
 * @author James (Juris) Puchin
 * Created on 4/10/2018
 */
public class OAuthClientManager extends DSLogger {
    private WebClient accessTokenService;
    private String userName;
    private String password;
    private long expireAt;
    private ClientAccessToken token;
    private final Object tokenLock;
    private Consumer consumer;

    /**
     * Manage OAuth2 for Client flow.
     */
    OAuthClientManager(String clientID, String clientSecret, String tokenURL) {
        this(clientID, clientSecret, null, null, tokenURL);
    }

    /**
     * Manage Oauth2 for Password flow.
     */
    OAuthClientManager(String clientID, String clientSecret, String user, String password, String tokenURL) {
        this.userName = user;
        this.password = password;
        consumer = new Consumer(clientID, clientSecret);
        accessTokenService = WebClient.create(tokenURL);
        tokenLock = new Object();
    }

    public ClientAccessToken getAccessToken() {
        synchronized (tokenLock) {
            if (token == null) {
                getNewToken();
            } else if (expireAt > 0 && System.currentTimeMillis() >= expireAt) {         //Refresh if expired
                refreshToken();
            }
        }
        return token;
    }

    private void getNewToken() {
        AccessTokenGrant grant;

        if (password != null || userName != null) {
            grant = new ResourceOwnerGrant(userName, password);
        } else {
            grant = new ClientCredentialsGrant();
        }

        try {
            token = OAuthClientUtils.getAccessToken(accessTokenService, consumer, grant);
            setExpirationTime();
        } catch (OAuthServiceException ex) {
            DSException.throwRuntime(ex);
        }

        info("New token generated. URI: " + accessTokenService.getCurrentURI());
    }

    private void refreshToken() {
        try {
            token = OAuthClientUtils.refreshAccessToken(accessTokenService, consumer, token);
            setExpirationTime();
        } catch (Exception ex) {
            info("Token refresh failed. URI: " + accessTokenService.getCurrentURI());
            info(ex.toString());
            getNewToken();
        }
        info("Token refreshed. URI: " + accessTokenService.getCurrentURI());
    }

    /**
     * Force token refresh or re-acquisition.
     */
    public void onAuthFailed() {
        synchronized (tokenLock) {
            String refreshToken = token.getRefreshToken();
            if (refreshToken != null) {
                refreshToken();
            } else {
                getNewToken();
            }
        }
    }

    private void setExpirationTime() {
        token.setIssuedAt(System.currentTimeMillis());
        token.setExpiresIn(TimeUnit.MILLISECONDS.convert(token.getExpiresIn(), TimeUnit.SECONDS));

        if (token.getExpiresIn() > 0) {
            expireAt = token.getIssuedAt() + token.getExpiresIn();
        }
    }

    public String createAuthorizationHeader() {
        return OAuthClientUtils.createAuthorizationHeader(getAccessToken());
    }
}
