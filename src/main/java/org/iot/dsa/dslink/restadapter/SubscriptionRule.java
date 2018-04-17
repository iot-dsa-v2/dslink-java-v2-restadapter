package org.iot.dsa.dslink.restadapter;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.iot.dsa.DSRuntime;
import org.iot.dsa.DSRuntime.Timer;
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
    
    private AbstractRuleNode node;
    private OutboundStream stream;
    private long lastUpdateTime = -1;
    private Timer future = null;
    
    private boolean valuesInBody = false;
    private List<String> urlParamsWithValues = new ArrayList<String>();
    
    private String subPath;
    private String restUrl;
    private String method;
    private DSMap urlParameters;
    private String body;
    private long minRefreshRate;
    private long maxRefreshRate;
    
    private int rowNum;
    
    public SubscriptionRule(AbstractRuleNode node, String subPath, String restUrl, String method, DSMap urlParameters, String body, double minRefreshRate, double maxRefreshRate, int rowNum) {
        this.node = node;
        this.subPath = subPath;
        this.restUrl = restUrl;
        this.method = method;
        this.urlParameters = urlParameters;
        this.body = body;
        this.minRefreshRate = (long) (minRefreshRate * 1000);
        this.maxRefreshRate = (long) (maxRefreshRate * 1000);
        this.rowNum = rowNum;
        
        learnPattern();
        DSRuntime.run(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }
    
    private void init() {
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
                if (str.indexOf(Constants.PLACEHOLDER_VALUE) != -1 || str.indexOf(Constants.PLACEHOLDER_TS) != -1 || str.indexOf(Constants.PLACEHOLDER_STATUS) != -1) {
                    urlParamsWithValues.add(entry.getKey());
                }
            }
        }
        if (body != null) {
            if (body.indexOf(Constants.PLACEHOLDER_VALUE) != -1 || body.indexOf(Constants.PLACEHOLDER_TS) != -1 || body.indexOf(Constants.PLACEHOLDER_STATUS) != -1) {
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
        if (lastUpdateTime < 0 || System.currentTimeMillis() - lastUpdateTime >= minRefreshRate) {
            if (future != null) {
                future.cancel();
            }
            sendUpdate(dateTime, value, status);
        }
    }
    
    public void sendUpdate(final DSDateTime dateTime, final DSElement value, final DSStatus status) {
        
        DSMap urlParams = urlParameters.copy();
        String body = this.body;
        for (String key: urlParamsWithValues) {
            String pattern = urlParams.getString(key);
            if (Constants.PLACEHOLDER_VALUE.equals(pattern)) {
                urlParams.put(key, value);
            } else {
                pattern = pattern.replaceAll(Constants.PLACEHOLDER_VALUE, value.toString());
                pattern = pattern.replaceAll(Constants.PLACEHOLDER_TS, dateTime.toString());
                pattern = pattern.replaceAll(Constants.PLACEHOLDER_STATUS, status.toString());
                urlParams.put(key, pattern);
            }
        }
        
        if (valuesInBody) {
            body = body.replaceAll(Constants.PLACEHOLDER_VALUE, value.toString());
            body = body.replaceAll(Constants.PLACEHOLDER_TS, dateTime.toString());
            body = body.replaceAll(Constants.PLACEHOLDER_STATUS, status.toString());
        }
        
        Response resp = getWebClientProxy().invoke(method, restUrl, urlParams, body);
        node.responseRecieved(resp, rowNum);
        
        lastUpdateTime = System.currentTimeMillis();
        if (maxRefreshRate > 0) {
            future = DSRuntime.runDelayed(new Runnable() {
                @Override
                public void run() {
                    sendUpdate(dateTime, value, status);
                }
            }, maxRefreshRate);
        }
    }
    
    public void close() {
        if (stream != null && stream.isStreamOpen()) {
            stream.closeStream();
        }
    }
    

    public WebClientProxy getWebClientProxy() {
        return node.getWebClientProxy();
    }

}
