package org.iot.dsa.dslink.restadapter;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.oauth2.client.OAuthClientUtils;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.util.DSException;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class WebClientProxy {

    private String username;
    private String password;
    private Util.AUTH_SCHEME scheme;
    private OAuthClientManager authManager;

    private WebClientProxy(String username, String password, Util.AUTH_SCHEME scheme, OAuthClientManager authMngr) {
        this.username = username;
        this.password = password;
        this.scheme = scheme;
        this.authManager = authMngr;
    }

    public static WebClientProxy buildNoAuthClient() {
        return new WebClientProxy(null, null, Util.AUTH_SCHEME.NO_AUTH, null);
    }

    public static WebClientProxy buildBasicUserPassClient(String username, String password) {
        return new WebClientProxy(username, password, Util.AUTH_SCHEME.BASIC_USR_PASS, null);
    }

    public static WebClientProxy buildClientFlowOAuth2Client(String clientID, String clientSecret, String tokenURL) {
        OAuthClientManager mngr = new OAuthClientManager(clientID, clientSecret, tokenURL);
        return new WebClientProxy(null, null, Util.AUTH_SCHEME.OAUTH2_CLIENT, mngr);
    }

    public static WebClientProxy buildPasswordFlowOAuth2Client(String username, String password, String clientID, String clientSecret, String tokenURL) {
        OAuthClientManager mngr = new OAuthClientManager(clientID, clientSecret, username, password, tokenURL);
        return new WebClientProxy(username, password, Util.AUTH_SCHEME.OAUTH2_USR_PASS, mngr);
    }

//    public Response get(String address, DSMap urlParameters) {
//        WebClient client = prepareWebClient(address, urlParameters);
//        Response r = client.get();
//        client.close();
//        return r;
//    }
//
//    public Response put(String address, DSMap urlParameters, Object body) {
//        WebClient client = prepareWebClient(address, urlParameters);
//        Response r = client.put(body);
//        client.close();
//        return r;
//    }
//
//    public Response post(String address, DSMap urlParameters, Object body) {
//        WebClient client = prepareWebClient(address, urlParameters);
//        Response r = client.post(body);
//        client.close();
//        return r;
//    }
//
//    public Response delete(String address, DSMap urlParameters) {
//        WebClient client = prepareWebClient(address, urlParameters);
//        Response r = client.delete();
//        client.close();
//        return r;
//    }
//
//    public Response patch(String address, DSMap urlParameters, Object body) {
//        return invoke("PATCH", address, urlParameters, body);
//    }

    public Response invoke(String httpMethod, String address, DSMap urlParameters, Object body) {
        WebClient client = prepareWebClient(address, urlParameters);
        if (authManager != null)
            client.header(HttpHeaders.AUTHORIZATION, authManager.createAuthorizationHeader());
        Response r = null;
        try {
            r = client.invoke(httpMethod, body);
        } catch (NotAuthorizedException ex) {
            if (authManager != null) {
                authManager.onAuthFailed();
                client.replaceHeader(HttpHeaders.AUTHORIZATION, authManager.createAuthorizationHeader());
                r = client.invoke(httpMethod, body);
            } else {
                DSException.throwRuntime(ex);
            }
        } finally {
            client.close();
        }
        return r;
    }

    private WebClient prepareWebClient(String address, DSMap urlParameters) {
        WebClient client = configureAuthorization(address);
        client.accept(MediaType.APPLICATION_JSON);
        client.type(MediaType.APPLICATION_JSON);
        for (int i = 0; i < urlParameters.size(); i++) {
            Entry entry = urlParameters.getEntry(i);
            Object value = Util.dsElementToObject(entry.getValue());
            client.query(entry.getKey(), value);
        }
        return client;
    }

    private WebClient configureAuthorization(String address) {
        WebClient client = null;
        switch (scheme) {
            case NO_AUTH:
                client = WebClient.create(address);
                break;
            case BASIC_USR_PASS:
                client = WebClient.create(address, username, password, null);
                break;
            case OAUTH2_CLIENT:
                client = WebClient.create(address);
                ClientAccessToken token = authManager.getAccessToken();
                client.header(HttpHeaders.AUTHORIZATION, OAuthClientUtils.createAuthorizationHeader(token));
                break;
            case OAUTH2_USR_PASS:
                //TODO: support USER/PASS
                DSException.throwRuntime(new RuntimeException("Unsupported authorization type!"));
                break;
        }
        return client;
    }
}
