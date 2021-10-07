package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.DSRuntime;
import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.requester.AbstractSubscribeHandler;
import org.iot.dsa.dslink.requester.ErrorType;
import org.iot.dsa.dslink.requester.OutboundStream;
import org.iot.dsa.dslink.requester.OutboundSubscribeHandler;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIValue;
import org.iot.dsa.node.DSLong;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;

public class TestSubscriptionRule extends AbstractSubscribeHandler implements OutboundSubscribeHandler {
    
    private String subpath;
    private TestRuleNode node;
//    private OutboundStream stream;

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
        requester.subscribe(this.subpath, DSLong.valueOf(qos), this);
        
    }

    @Override
    public void onClose() {
        super.onClose();
        node.debug("Test Rule with sub path " + subpath + ": onClose called");
//        close();
    }

    @Override
    public void onError(ErrorType type, String msg) {
        super.onError(type, msg);
        node.debug("Test Rule with sub path " + subpath + ": onError called with msg " + msg);
//        DSException.throwRuntime(new RuntimeException(msg));
    }

    @Override
    public void onInit(String path, DSIValue qos, OutboundStream stream) {
        super.onInit(path, qos, stream);
        node.debug("Test Rule with sub path " + subpath + ": onInit called");
//        this.stream = stream; 
    }

    @Override
    public void onUpdate(DSDateTime dateTime, DSElement value, DSStatus status) {
        node.debug("Test Rule with sub path " + subpath + ": onUpdate called with value " + (value!=null ? value : "Null"));
    }
    
    public void close() {
        if (!isClosed() && getStream() != null) {
            node.debug("Test Rule with sub path " + subpath + ": closing Stream");
            getStream().closeStream();
        }
    }

}
