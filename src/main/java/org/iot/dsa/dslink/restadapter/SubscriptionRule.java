package org.iot.dsa.dslink.restadapter;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.requester.ErrorType;
import org.iot.dsa.dslink.requester.OutboundStream;
import org.iot.dsa.dslink.requester.OutboundSubscribeHandler;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;
import org.iot.dsa.util.DSException;

public class SubscriptionRule extends DSNode implements OutboundSubscribeHandler {
    
    private DSMap parameters;
    private OutboundStream stream;
    
    private boolean valuesInBody = false;
    private List<String> urlParamsWithValues = new ArrayList<String>();
    
    public SubscriptionRule() {
        
    }
    
    public SubscriptionRule(DSMap parameters) {
        this.parameters = parameters;
    }
    
    @Override
    protected void onStable() {
        DSIRequester requester = MainNode.getRequester();
        
        String path = getSubscribePath();
        int qos = 0;
        requester.subscribe(path, qos, this);
        learnPattern();
    }
    
    private void learnPattern() {
        DSMap urlParams = getURLParameters();
        for (int i = 0; i < urlParams.size(); i++) {
            Entry entry = urlParams.getEntry(i);
            DSElement val = entry.getValue();
            if (val.isString()) {
                String str = val.toString();
                if (str.indexOf("%VALUE%") != -1 || str.indexOf("%TIMESTAMP%") != -1 || str.indexOf("%STATUS%") != -1) {
                    urlParamsWithValues.add(entry.getKey());
                }
            }
        }
        String body = getBody();
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
        DSMap urlParams = getURLParameters().copy();
        String body = getBody();
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
        
        Response resp = WebClientProxy.invoke(getMethod(), getRestUrl(), urlParams, body);
        info(resp.getEntity());
    }
    
    public void close() {
        if (stream != null && stream.isStreamOpen()) {
            stream.closeStream();
        }
    }
    
    public String getSubscribePath() {
        return parameters.getString("Subscribe Path");
    }
    
    public String getRestUrl() {
        return parameters.getString("REST URL");
    }
    
    public String getMethod() {
        return parameters.getString("Method");
    }
    
    public DSMap getURLParameters() {
        return parameters.getMap("URL Parameters");
    }
    
    public String getBody() {
        return parameters.getString("Body");
    }

}
