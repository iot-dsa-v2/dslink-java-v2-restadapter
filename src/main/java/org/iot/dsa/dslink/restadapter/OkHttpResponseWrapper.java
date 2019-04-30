package org.iot.dsa.dslink.restadapter;

import java.io.IOException;
import org.iot.dsa.logging.DSLogger;
import org.iot.dsa.time.DSDateTime;
import okhttp3.Response;

public class OkHttpResponseWrapper extends DSLogger implements ResponseWrapper {
    
    private Response response;
    private String body = null;
    
    public OkHttpResponseWrapper(Response response) {
        this.response = response;
    }
    
    public Response getResponse() {
        return response;
    }

    @Override
    public int getCode() {
        return response.code();
    }

    @Override
    public String getData() {
        if (body == null) {
            try {
                body = response.body().string();
            } catch (IOException e) {
                warn("", e);
            } finally {
                response.close();
            }
        }
        return body;
    }

    @Override
    public DSDateTime getTS() {
        return DSDateTime.valueOf(response.receivedResponseAtMillis());
    }

}
