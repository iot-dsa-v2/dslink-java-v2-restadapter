package org.iot.dsa.dslink.restadapter;

import org.apache.commons.lang3.RandomStringUtils;
import org.iot.dsa.node.DSBool;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSLong;
import org.iot.dsa.node.DSNode;

public abstract class AbstractRuleNode extends DSNode {
    private DSInfo bufferEnabled = getInfo(Constants.USE_BUFFER);
    private DSInfo maxBatchSize = getInfo(Constants.MAX_BATCH_SIZE);
    private String id;

    public WebClientProxy getWebClientProxy() {
        return ((ConnectionNode) getParent()).getWebClientProxy();
    }

    public abstract void responseRecieved(ResponseWrapper resp, int rowNum);
    
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.USE_BUFFER, DSBool.FALSE, "Whether updates that failed to send should be stored and re-sent in the future");
        declareDefault(Constants.MAX_BATCH_SIZE, DSLong.valueOf(50), "Maximum number of updates to put in a single REST request");
    }
    
    public boolean isBufferEnabled() {
        return bufferEnabled.getValue().toElement().toBoolean();
    }
    
    public int getMaxBatchSize() {
        return maxBatchSize.getValue().toElement().toInt();
    }
    
    @Override
    protected void onStarted() {
        super.onStarted();
        DSIObject idobj = get("id");
        if (idobj instanceof DSIValue) {
            id = ((DSIValue) idobj).toElement().toString();
        } else {
            id = RandomStringUtils.randomAlphanumeric(12);
            put("id", id);
        }
    }
    
    public String getId() {
        return id;
    }

}
