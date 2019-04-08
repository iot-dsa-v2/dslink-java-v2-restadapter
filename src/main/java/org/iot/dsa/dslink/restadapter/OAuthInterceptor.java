package org.iot.dsa.dslink.restadapter;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

/**
 * @author Daniel Shapiro
 */

public class OAuthInterceptor implements Interceptor {

    private final OAuthTokenUpdater updater;
    private final WebClientProxy home;

    public OAuthInterceptor(WebClientProxy home) {
        this.home = home;
        this.updater = new OAuthTokenUpdater(home);
    }
    
    private String constructAuthHeader(String token) {
        return token != null? String.format("Bearer %s", token) : null;
    }

    private void setAuthHeader(Request.Builder builder, String header) {
        if (header != null) //Add Auth token to each request if authorized
            builder.header("Authorization", header);
    }

    private int updateToken() {
        //Refresh token, synchronously, save it, and return result code
        //you might use retrofit here
        return updater.updateToken();
    }

    private String getToken() {
        return updater.getToken();
    }
    
//    private int responseCount(Response response) {
//        int result = 1;
//        while ((response = response.priorResponse()) != null) {
//          result++;
//        }
//        return result;
//      }

//    @Override
//    public Request authenticate(Route route, Response response) throws IOException {
//        if (responseCount(response) >= 3) {
//            return null; // If we've failed 3 times, give up.
//        }
//        String token = getToken();
//        String authHeader = constructAuthHeader(token);
//        if (authHeader == null || authHeader.equals(response.request().header("Authorization"))) {
//            // If no token or if we already failed with this token, update the token
//            int code = updateToken() / 100; //refresh token
//            if (code != 2) { //if refresh token failed for some reason, give up
//                return null;
//            }
//            token = getToken();
//            authHeader = constructAuthHeader(token);
//        }
//        Request.Builder builder = response.request().newBuilder();
//        builder.header("Accept", "application/json"); //if necessary, say to consume JSON
//        setAuthHeader(builder, authHeader); //write current token to request
//        return builder.build();
//    }
    
    private Request constructAuthRequest(Request originalRequest) {
        String token = getToken();
        String authHeader = constructAuthHeader(token);
        Request.Builder builder = originalRequest.newBuilder();
        builder.header("Accept", "application/json");
        setAuthHeader(builder, authHeader);
        return builder.build();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request authRequest = constructAuthRequest(originalRequest);
        Response origResponse = chain.proceed(authRequest);
        if (origResponse.code() == 401 || origResponse.code() == 403) {
            synchronized (home.getClient()) {
                int code = updateToken() / 100; //refresh token
                if (code != 2) { //if refresh token failed for some reason, give up
                    return origResponse;
                }
                Request newAuthRequest = constructAuthRequest(originalRequest);
                origResponse.close();
                Response newResponse = chain.proceed(newAuthRequest);
                return newResponse;
            }
        } else {
            return origResponse;
        }
    }
    
    
}
