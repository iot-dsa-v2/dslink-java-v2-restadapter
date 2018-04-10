package org.iot.dsa.dslink.restadapter;

import java.util.ArrayList;
import java.util.List;
import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.requester.ErrorType;
import org.iot.dsa.dslink.requester.OutboundStream;
import org.iot.dsa.dslink.requester.OutboundSubscribeHandler;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;
import org.iot.dsa.util.DSException;

public class SubscriptionRule implements OutboundSubscribeHandler {
    
    private ConnectionNode conn;
    private OutboundStream stream;
    
    private boolean valuesInBody = false;
    private List<String> urlParamsWithValues = new ArrayList<String>();
    
    private String subPath;
    private String restUrl;
    private String method;
    private DSMap urlParameters;
    private String body;
    
    public SubscriptionRule(ConnectionNode conn, String subPath, String restUrl, String method, DSMap urlParameters, String body) {
        this.conn = conn;
        this.subPath = subPath;
        this.restUrl = restUrl;
        this.method = method;
        this.urlParameters = urlParameters;
        this.body = body;
        
        learnPattern();
        DSIRequester requester = MainNode.getRequester();
        int qos = 0;
        requester.subscribe(this.subPath, qos, this);
    }
    
    private void learnPattern() {
        for (int i = 0; i < urlParameters.size(); i++) {
            Entry entry = urlParameters.getEntry(i);
            DSElement val = entry.getValue();
            if (val.isString()) {
                String str = val.toString();
                if (str.indexOf("%VALUE%") != -1 || str.indexOf("%TIMESTAMP%") != -1 || str.indexOf("%STATUS%") != -1) {
                    urlParamsWithValues.add(entry.getKey());
                }
            }
        }
        if (body != null) {
            if (body.indexOf("%VALUE%") != -1 || body.indexOf("%TIMESTAMP%") != -1 || body.indexOf("%STATUS%") != -1) {
                valuesInBody = true;
            }
        }
    }

    @Override
    public void onClose() {
       close();
    }

    @Override
    public void onError(ErrorType type, String msg) {
        DSException.throwRuntime(new RuntimeException(msg));
    }

    @Override
    public void onInit(String path, int qos, OutboundStream stream) {
        this.stream = stream;
    }

    @Override
    public void onUpdate(DSDateTime dateTime, DSElement value, DSStatus status) {
        DSMap urlParams = urlParameters.copy();
        String body = this.body;
        for (String key: urlParamsWithValues) {
            String pattern = urlParams.getString(key);
            if ("%VALUE%".equals(pattern)) {
                urlParams.put(key, value);
            } else {
                pattern = pattern.replaceAll("%VALUE%", value.toString());
                pattern = pattern.replaceAll("%TIMESTAMP%", dateTime.toString());
                pattern = pattern.replaceAll("%STATUS%", status.toString());
                urlParams.put(key, pattern);
            }
        }
        
        if (valuesInBody) {
            body = body.replaceAll("%VALUE%", value.toString());
            body = body.replaceAll("%TIMESTAMP%", dateTime.toString());
            body = body.replaceAll("%STATUS%", status.toString());
        }
        
        getWebClientProxy().invoke(method, restUrl, urlParams, body);
//        info(resp.getEntity());
    }
    
    public void close() {
        if (stream != null && stream.isStreamOpen()) {
            stream.closeStream();
        }
    }
    

    public WebClientProxy getWebClientProxy() {
        return conn.getWebClientProxy();
    }

}
