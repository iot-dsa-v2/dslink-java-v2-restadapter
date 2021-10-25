package org.iot.dsa.dslink.restadapter;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class BearerAuthInterceptor implements Interceptor {

    private String token;

    public BearerAuthInterceptor(String token) {
        this.token = token;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder()
                    .header("Authorization", "Bearer " + token).build();
        return chain.proceed(authenticatedRequest);
    }

}
