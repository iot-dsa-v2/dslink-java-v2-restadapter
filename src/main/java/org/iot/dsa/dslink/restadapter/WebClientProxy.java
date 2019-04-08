package org.iot.dsa.dslink.restadapter;


import org.iot.dsa.logging.DSLogger;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebClientProxy extends DSLogger {
    private CredentialProvider credentials;

    private OkHttpClient client;

    public WebClientProxy(CredentialProvider credentials) {
        this.credentials = credentials;
    }

//    public static WebClientProxy buildNoAuthClient() {
//        return new WebClientProxy(null, null, null, null, null, Util.AUTH_SCHEME.NO_AUTH);
//    }
//
//    public static WebClientProxy buildBasicUserPassClient(String username, String password) {
//        return new WebClientProxy(username, password, null, null, null, Util.AUTH_SCHEME.BASIC_USR_PASS);
//    }
//
//    public static WebClientProxy buildClientFlowOAuth2Client(String clientID, String clientSecret, String tokenURL) {
//        return new WebClientProxy(null, null, clientID, clientSecret, tokenURL, Util.AUTH_SCHEME.OAUTH2_CLIENT);
//    }
//
//    public static WebClientProxy buildPasswordFlowOAuth2Client(String username, String password, String clientID, String clientSecret, String tokenURL) {
//        return new WebClientProxy(username, password, clientID, clientSecret, tokenURL, Util.AUTH_SCHEME.OAUTH2_USR_PASS);
//    }
    

    public Response invoke(String httpMethod, String address, DSMap urlParameters, Object body) {
        prepareClient();
        Request.Builder requestBuilder = prepareRequest(address, urlParameters);
        requestBuilder.method(httpMethod, body == null ? null : RequestBody.create(MediaType.parse("application/json"), body.toString()));
        Request request = requestBuilder.build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (Exception e) {
            error("", e);
        }
        return response;
    }
    
    private Request.Builder prepareRequest(String address, DSMap urlParameters) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(address).newBuilder();

        for (Entry entry: urlParameters) {
            Object value = Util.dsElementToObject(entry.getValue());
            urlBuilder.addQueryParameter(entry.getKey(), value.toString());
        }
        Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json");
        return requestBuilder;
    }
    
    private void prepareClient() {
        if (client == null) {
            client  = configureAuthorization();
        }
    }

    private OkHttpClient configureAuthorization() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        switch (getScheme()) {
            case NO_AUTH:
                break;
            case BASIC_USR_PASS:
                //TODO implement basic auth
                break;
            case OAUTH2_CLIENT:
            case OAUTH2_USR_PASS:
                clientBuilder.addInterceptor(new OAuthInterceptor(this));
//                client.header(HttpHeaders.AUTHORIZATION, authManager.createAuthorizationHeader());
                break;
        }
        return clientBuilder.build();
    }
    
    public OkHttpClient getClient() {
        if (client == null) {
            prepareClient();
        }
        return client;
    }
    
    public String getUsername() {
        return credentials.getUsername();
    }

    public String getPassword() {
        return credentials.getPassword();
    }
    
    public String getClientID() {
        return credentials.getClientId();
    }

    public String getClientSecret() {
        return credentials.getClientSecret();
    }

    public String getTokenURL() {
        return credentials.getTokenURL();
    }
    
    public Util.AUTH_SCHEME getScheme() {
        return credentials.getAuthScheme();
    }
    
}
