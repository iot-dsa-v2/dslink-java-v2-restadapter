package org.iot.dsa.dslink.restadapter;


import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.iot.dsa.io.json.JsonReader;
import org.iot.dsa.logging.DSLogger;
import org.iot.dsa.node.DSMap;
import java.io.IOException;

/**
 * @author James (Juris) Puchin
 * Created on 3/29/2018
 */
public class OAuthTokenUpdater extends DSLogger {

    private OkHttpClient tokenUpdateClient;
    private String token = null;
    private final Object tokenLock = new Object();
    private WebClientProxy home;
    private String refreshToken = null;
    private static final MediaType MEDIA_TYPE_URL = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    public void clearTokens() {
        synchronized (tokenLock) {
            token = null;
            refreshToken = null;
        }
    }

    public OAuthTokenUpdater(WebClientProxy home) {
        this.tokenUpdateClient = new OkHttpClient();
        this.home = home;
    }

    private void extractToken(String raw) {
        JsonReader reader = new JsonReader(raw);
        DSMap map = reader.getMap();
        synchronized (tokenLock) {
            token = map.getString("access_token");
            refreshToken = map.getString("refresh_token");
        }
        reader.close();
    }

    private String makeTokenRequest() {
        StringBuilder sb = new StringBuilder();
        sb.append("client_id=");
        sb.append(getID());
        sb.append("&client_secret=");
        sb.append(getSecret());
        if (Util.AUTH_SCHEME.OAUTH2_USR_PASS.equals(getScheme())) {
            sb.append("&username=");
            sb.append(getUser());
            sb.append("&password=");
            sb.append(getPassword());
            sb.append("&grant_type=password");
        } else {
            sb.append("&grant_type=client_credentials");
        }
        return sb.toString();
    }

    private String makeTokenRefreshRequest() {
        StringBuilder sb = new StringBuilder();
        sb.append("client_id=");
        sb.append(getID());
        sb.append("&client_secret=");
        sb.append(getSecret());
        sb.append("&refresh_token=");
        sb.append(getRefreshToken());
        sb.append("&grant_type=refresh_token");
        return sb.toString();
    }

    public int updateToken() {
        if (getID() == null || getSecret() == null) {
            return 401;
        }
        String reqString;
        if (refreshToken == null) {
            reqString = makeTokenRequest();
        } else {
            reqString = makeTokenRefreshRequest();
        }
        Request request = new Request.Builder()
                .url(getTokenURL())
                .post(RequestBody.create(MEDIA_TYPE_URL, reqString))
                .build();
        info(request.toString());
        String rawResponse = null;
        int code = 404;
        try {
            Response response = tokenUpdateClient.newCall(request).execute();
            if (!response.isSuccessful())
                throw new IOException("Unexpected code " + response);
            rawResponse = response.body().string();
            code = response.code();
        } catch (Exception e) {
            warn("", e);
            return code;
        }
        extractToken(rawResponse);
        return code;
    }

    public String getToken() {
        synchronized (tokenLock) {
            return token;
        }
    }

    public String getRefreshToken() {
        synchronized (tokenLock) {
            return refreshToken;
        }
    }

    private String getID() {
        return home.getClientID();
    }

    private String getSecret() {
        return home.getClientSecret();
    }
    
    private String getPassword() {
        return home.getPassword();
    }

    private String getUser() {
        return home.getUsername();
    }

    private String getTokenURL() {
        return home.getTokenURL();
    }
    
    private Util.AUTH_SCHEME getScheme() {
        return home.getScheme();
    }
}
