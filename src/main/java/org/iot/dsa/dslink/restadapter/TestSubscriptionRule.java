package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.DSRuntime;
import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.requester.ErrorType;
import org.iot.dsa.dslink.requester.OutboundStream;
import org.iot.dsa.dslink.requester.OutboundSubscribeHandler;
import org.iot.dsa.logging.DSLogger;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;
import org.iot.dsa.util.DSException;

public class TestSubscriptionRule extends DSLogger implements OutboundSubscribeHandler {
    
    private String subpath;
    private TestRuleNode node;
    private OutboundStream stream;

    public TestSubscriptionRule(TestRuleNode testRuleNode, String subpath) {
        this.subpath = subpath;
        this.node = testRuleNode;
        DSRuntime.run(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    protected void init() {
        DSIRequester requester = MainNode.getRequester();
        int qos = 0;
        requester.subscribe(this.subpath, qos, this);
        
    }

    @Override
    public void onClose() {
        info("Test Rule with sub path " + subpath + ": onClose called");
        close();
    }

    @Override
    public void onError(ErrorType type, String msg) {
        info("Test Rule with sub path " + subpath + ": onError called with msg " + msg);
        DSException.throwRuntime(new RuntimeException(msg));
    }

    @Override
    public void onInit(String path, DSIValue qos, OutboundStream stream) {
        info("Test Rule with sub path " + subpath + ": onInit called");
        this.stream = stream; 
    }

    @Override
    public void onUpdate(DSDateTime dateTime, DSElement value, DSStatus status) {
        info("Test Rule with sub path " + subpath + ": onUpdate called with value " + (value!=null ? value : "Null"));
        
    }
    
    public void close() {
        if (stream != null && stream.isStreamOpen()) {
            info("Test Rule with sub path " + subpath + ": closing Stream");
            stream.closeStream();
        }
    }

}
