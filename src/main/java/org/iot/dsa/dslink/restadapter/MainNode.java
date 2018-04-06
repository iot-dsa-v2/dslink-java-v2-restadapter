package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.DSLinkConnection;
import org.iot.dsa.dslink.DSMainNode;
import org.iot.dsa.dslink.DSLinkConnection.Listener;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;
/**
 * The root and only node of this link.
 *
 * @author Aaron Hansen
 */
public class MainNode extends DSMainNode {


    private static DSIRequester requester;

    public MainNode() {
    }

    
    /**
     * Defines the permanent children of this node type, their existence is guaranteed in all
     * instances.  This is only ever called once per, type per process.
     */
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Add Rule", makeAddRuleAction());
    }

    private DSAction makeAddRuleAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((MainNode) info.getParent()).addRule(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Subscribe Path", DSValueType.STRING, null);
        act.addParameter("REST URL", DSValueType.STRING, null);
        act.addDefaultParameter("Method", DSString.valueOf("POST"), null);
        act.addDefaultParameter("URL Parameters", new DSMap(), null);
        act.addParameter("Body", DSValueType.STRING, null);
        return act;
    }


    protected void addRule(DSMap parameters) {
        String name = parameters.getString("Name");
        put(name, new SubscriptionRule(parameters)).setTransient(true);
    }
    
    @Override
    protected void onStarted() {
        getLink().getConnection().addListener(new Listener() {
            @Override
            public void onConnect(DSLinkConnection connection) {
                MainNode.setRequester(getLink().getConnection().getRequester());
            }

            @Override
            public void onDisconnect(DSLinkConnection connection) {
            }
        });
    }

    public static DSIRequester getRequester() {
        return requester;
    }

    public static void setRequester(DSIRequester requester) {
        MainNode.requester = requester;
    }
}
