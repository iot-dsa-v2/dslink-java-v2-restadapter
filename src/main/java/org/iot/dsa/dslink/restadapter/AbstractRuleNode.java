package org.iot.dsa.dslink.restadapter;

import javax.ws.rs.core.Response;
import org.iot.dsa.node.DSNode;

public abstract class AbstractRuleNode extends DSNode {

    public WebClientProxy getWebClientProxy() {
        return ((ConnectionNode) getParent()).getWebClientProxy();
    }

    public abstract void responseRecieved(Response resp, int rowNum);

}
