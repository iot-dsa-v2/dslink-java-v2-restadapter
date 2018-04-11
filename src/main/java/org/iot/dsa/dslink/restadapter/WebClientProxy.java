package org.iot.dsa.dslink.restadapter;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.iot.dsa.DSRuntime;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.util.DSException;

public class WebClientProxy {

    private String username;
    private String password;
    private String clientID;
    private String clientSecret;
    private String tokenURL;
    private Util.AUTH_SCHEME scheme;

    private WebClientProxy(String username, String password, String clientID, String clientSecret, String tokenURL, Util.AUTH_SCHEME scheme) {
        this.username = username;
        this.password = password;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.tokenURL = tokenURL;
        this.scheme = scheme;
    }

    public static WebClientProxy buildNoAuthClient() {
        return new WebClientProxy(null, null, null, null, null, Util.AUTH_SCHEME.NO_AUTH);
    }
    
    public static WebClientProxy buildBasicUserPassClient(String username, String password) {
        return new WebClientProxy(username, password, null, null, null, Util.AUTH_SCHEME.BAISC_USR_PASS);
    }

    public static WebClientProxy buildClientFlowOAuth2Client(String clientID, String clientSecret, String tokenURL) {
        return new WebClientProxy(null, null, clientID, clientSecret, tokenURL, Util.AUTH_SCHEME.OAUTH2_CLIENT);
    }

    public static WebClientProxy buildPasswordFlowOAuth2Client(String username, String password, String clientID, String clientSecret, String tokenURL) {
        return new WebClientProxy(username, password, clientID, clientSecret, tokenURL, Util.AUTH_SCHEME.OAUTH2_USR_PASS);
    }
	
	public Response get(String address, DSMap urlParameters) {
		WebClient client = prepareWebClient(address, urlParameters);
		Response r = client.get();
		client.close();
		return r;
	}
	
	public Response put(String address, DSMap urlParameters, Object body) {
	    WebClient client = prepareWebClient(address, urlParameters);
	    Response r = client.put(body);
	    client.close();
	    return r;
	}
	
	public Response post(String address, DSMap urlParameters, Object body) {
        WebClient client = prepareWebClient(address, urlParameters);
        Response r = client.post(body);
        client.close();
        return r;
    }
	
	public Response delete(String address, DSMap urlParameters) {
        WebClient client = prepareWebClient(address, urlParameters);
        Response r = client.delete();
        client.close();
        return r;
    }
	
	public Response patch(String address, DSMap urlParameters, Object body) {
        return invoke("PATCH", address, urlParameters, body);
    }
	
	public Response invoke(String httpMethod, String address, DSMap urlParameters, Object body) {
	    WebClient client = prepareWebClient(address, urlParameters);
        Response r = client.invoke(httpMethod, body);
        client.close();
        return r;
	}
	
	private WebClient prepareWebClient(String address, DSMap urlParameters) {
	    WebClient client = configureAuthorization(address);
        client.accept(MediaType.APPLICATION_JSON);
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
                client =  WebClient.create(address);
                break;
            case BAISC_USR_PASS:
                client = WebClient.create(address, username, password, null);
                break;
            case OAUTH2_CLIENT:
            case OAUTH2_USR_PASS:
                DSException.throwRuntime(new RuntimeException("Unsupported authorization type!"));
                break;
        }
        return client;
    }
}
