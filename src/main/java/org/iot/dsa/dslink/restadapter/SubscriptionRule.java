package org.iot.dsa.dslink.restadapter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.iot.dsa.DSRuntime;
import org.iot.dsa.DSRuntime.Timer;
import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.requester.AbstractSubscribeHandler;
import org.iot.dsa.dslink.requester.ErrorType;
import org.iot.dsa.dslink.requester.OutboundStream;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSLong;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;
import okhttp3.Response;

public class SubscriptionRule extends AbstractSubscribeHandler implements UpdateSender {
    
    private AbstractRuleNode node;
    //private OutboundStream stream;
    private long lastUpdateTime = -1;
    private Timer future = null;
    private SubUpdate storedUpdate;
    private boolean unsentInBuffer = false;
    
    private boolean valuesInBody = false;
    private boolean batchable = false;
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
        DSIRequester requester = node.getRequester();
        int qos = 0;
        requester.subscribe(this.subPath, DSLong.valueOf(qos), this);
    }
    
    private void learnPattern() {
        for (Entry entry : urlParameters) {
            DSElement val = entry.getValue();
            if (val.isString()) {
                String str = val.toString();
                if (str.indexOf(Constants.PLACEHOLDER_VALUE) != -1 || str.indexOf(Constants.PLACEHOLDER_TS) != -1 || str.indexOf(Constants.PLACEHOLDER_STATUS) != -1) {
                    urlParamsWithValues.add(entry.getKey());
                }
            }
        }
        int indexOfValue = body.indexOf(Constants.PLACEHOLDER_VALUE);
        int indexOfTs = body.indexOf(Constants.PLACEHOLDER_TS);
        int indexOfStatus = body.indexOf(Constants.PLACEHOLDER_STATUS);
        if (body != null) {
            if (indexOfValue != -1 || indexOfTs != -1 || indexOfStatus != -1) {
                valuesInBody = true;
                int indexOfStart = body.indexOf(Constants.PLACEHOLDER_BLOCK_START);
                int indexOfEnd = body.indexOf(Constants.PLACEHOLDER_BLOCK_END);
                if (urlParamsWithValues.isEmpty() 
                        && indexOfStart != -1 && indexOfEnd != -1 
                        && (indexOfValue == -1 || indexOfStart < indexOfValue)
                        && (indexOfTs == -1 || indexOfStart < indexOfTs)
                        && (indexOfStatus == -1 || indexOfStart < indexOfStatus)
                        && indexOfValue < indexOfEnd 
                        && indexOfTs < indexOfEnd 
                        && indexOfStatus < indexOfEnd) {
                    batchable = true;
                }
            }
        }
    }

    @Override
    public void onClose() {
       super.onClose();
       node.info("Rule with sub path " + subPath + ": onClose called");
//       close();
    }

    @Override
    public void onError(ErrorType type, String msg) {
        super.onError(type, msg);
        node.info("Rule with sub path " + subPath + ": onError called with msg " + msg);
//        DSException.throwRuntime(new RuntimeException(msg));
    }

    @Override
    public void onInit(String path, DSIValue qos, OutboundStream stream) {
        super.onInit(path, qos, stream);
        node.info("Rule with sub path " + subPath + ": onInit called");
        //this.stream = stream;
    }

    @Override
    public void onUpdate(DSDateTime dateTime, DSElement value, DSStatus status) {
        node.info("Rule with sub path " + subPath + ": onUpdate called with value " + (value!=null ? value : "Null"));
        storedUpdate = new SubUpdate(dateTime.toString(), value.toString(), status.toString(), dateTime.timeInMillis());
        if (lastUpdateTime < 0 || System.currentTimeMillis() - lastUpdateTime >= minRefreshRate) {
            if (future != null) {
                future.cancel();
            }
            trySendUpdate(new SubUpdate(dateTime.toString(), value.toString(), status.toString(), dateTime.timeInMillis()));
        }
    }
    
    private void sendStoredUpdate() {
        if (storedUpdate != null) {
            trySendUpdate(storedUpdate);
        }
    }
    
    private String getSubId() {
        return node.getId() + "_" + subPath;
    }
    
    private void trySendUpdate(final SubUpdate update) {
        if (sendUpdate(update)) {
            if (unsentInBuffer) {
                unsentInBuffer = !Util.processBuffer(getSubId(), this);
            }
        } else {
            if (node.isBufferEnabled()) {
                Util.storeInBuffer(getSubId(), update);
                unsentInBuffer = true;
            }
        }
        
        lastUpdateTime = System.currentTimeMillis();
        if (maxRefreshRate > 0) {
            future = DSRuntime.runDelayed(new Runnable() {
                @Override
                public void run() {
                    sendStoredUpdate();
                }
            }, maxRefreshRate);
        }
    }
    
    public boolean sendUpdate(final SubUpdate update) {
        
        DSMap urlParams = urlParameters.copy();
        String body = this.body;
        for (String key: urlParamsWithValues) {
            String pattern = urlParams.getString(key);
            if (Constants.PLACEHOLDER_VALUE.equals(pattern)) {
                urlParams.put(key, update.value);
            } else {
                pattern = pattern.replaceAll(Constants.PLACEHOLDER_VALUE, update.value);
                pattern = pattern.replaceAll(Constants.PLACEHOLDER_TS, update.dateTime);
                pattern = pattern.replaceAll(Constants.PLACEHOLDER_STATUS, update.status);
                urlParams.put(key, pattern);
            }
        }
        
        if (valuesInBody) {
            body = body.replaceAll(Constants.PLACEHOLDER_VALUE, update.value);
            body = body.replaceAll(Constants.PLACEHOLDER_TS, update.dateTime);
            body = body.replaceAll(Constants.PLACEHOLDER_STATUS, update.status);
            body = body.replaceAll(Constants.PLACEHOLDER_BLOCK_START, "");
            body = body.replaceAll(Constants.PLACEHOLDER_BLOCK_END, "");
        }
        
        node.info("Rule with sub path " + subPath + ": sending Update with value " + (update.value!=null ? update.value : "Null"));
        
        ResponseWrapper resp = doSend(urlParams, body);
        return resp != null && resp.getCode() / 100 == 2;
    }
    
    @Override
    public Queue<SubUpdate> sendBatchUpdate(Queue<SubUpdate> updates) {
        if (!batchable) {
            Queue<SubUpdate> failed = new LinkedList<SubUpdate>();
            while (!updates.isEmpty()) {
                SubUpdate update = updates.poll();
                if (!sendUpdate(update)) {
                    failed.add(update);
                }
            }
            return failed;
        }
        DSMap urlParams = urlParameters.copy();
        StringBuilder sb = new StringBuilder();
        int indexOfStart = body.indexOf(Constants.PLACEHOLDER_BLOCK_START);
        int indexOfEnd = body.indexOf(Constants.PLACEHOLDER_BLOCK_END);
        String prefix = body.substring(0, indexOfStart);
        String block = body.substring(indexOfStart + Constants.PLACEHOLDER_BLOCK_START.length(), indexOfEnd);
        String suffix = body.substring(indexOfEnd + Constants.PLACEHOLDER_BLOCK_END.length());
        sb.append(prefix);
        Queue<SubUpdate> updatesCopy = new LinkedList<SubUpdate>();
        while (!updates.isEmpty()) {
            SubUpdate update = updates.poll();
            updatesCopy.add(update);
            String temp = block.replaceAll(Constants.PLACEHOLDER_VALUE, update.value)
                    .replaceAll(Constants.PLACEHOLDER_TS, update.dateTime)
                    .replaceAll(Constants.PLACEHOLDER_STATUS, update.status);
            sb.append(temp);
            if (!updates.isEmpty()) {
                sb.append(',');
            }
        }
        sb.append(suffix);
        String body = sb.toString();
        node.info("Rule with sub path " + subPath + ": sending batch update");
        
        ResponseWrapper resp = doSend(urlParams, body);
        if (resp != null && resp.getCode() / 100 == 2) {
            return null;
        } else {
            return updatesCopy;
        }
        
    }
    
    protected ResponseWrapper doSend(DSMap urlParams, String body) {
        Response resp = null;
        try {
            resp = getWebClientProxy().invoke(method, restUrl, urlParams, body);
        } catch (Exception e) {
            node.warn("", e);
        }
        ResponseWrapper respWrap = new OkHttpResponseWrapper(resp);
        node.responseRecieved(respWrap, rowNum);
        return respWrap;
    }
    
    public void close() {
        if (!isClosed() && getStream() != null) {
            node.info("Rule with sub path " + subPath + ": closing Stream");
            getStream().closeStream();
        }
    }
    
    public int getMaxBatchSize() {
        return node.getMaxBatchSize();
    }

    public WebClientProxy getWebClientProxy() {
        return node.getWebClientProxy();
    }
    
    public AbstractRuleNode getNode() {
        return node;
    }

}
