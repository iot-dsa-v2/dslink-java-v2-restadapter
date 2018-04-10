package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;

public class ConnectionNode extends DSNode {
    
    private DSMap parameters;
    private WebClientProxy clientProxy;
    
    public ConnectionNode() {
        
    }
    
    public ConnectionNode(DSMap parameters) {
        this.parameters = parameters;
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Remove", makeRemoveAction());
        declareDefault("Add Rule", makeAddRuleAction());
//        declareDefault("Add Rule Table", makeAddRuleTableAction());
    }
    
    @Override
    protected void onStarted() {
        if (this.parameters == null) {
            DSIObject o = get("parameters");
            if (o instanceof DSMap) {
                this.parameters = (DSMap) o;
            }
        } else {
            put("parameters", parameters.copy()).setHidden(true);
        }
    }
    
    @Override
    protected void onStable() {
        clientProxy = new WebClientProxy(getUsername(), getPassword());
        put("Edit", makeEditAction());
    }

    private DSAction makeAddRuleAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((ConnectionNode) info.getParent()).addRule(invocation.getParameters());
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
        put(name, new RuleNode(parameters)).setTransient(true);
    }
    
    private DSAction makeRemoveAction() {
        return new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((ConnectionNode) info.getParent()).delete();
                return null;
            }
        };
    }

    private void delete() {
        getParent().remove(getInfo());
    }
    
    private DSIObject makeEditAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((ConnectionNode) info.getParent()).edit(invocation.getParameters());
                return null;
            }
        };
        act.addDefaultParameter("Username", DSString.valueOf(getUsername()), null);
        act.addDefaultParameter("Password", DSString.valueOf(getPassword()), null).setEditor("password");
        return act;
    }

    protected void edit(DSMap parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            Entry entry = parameters.getEntry(i);
            this.parameters.put(entry.getKey(), entry.getValue().copy());
        }
        put("parameters", parameters.copy());
        onStable();
    }
    
    public String getUsername() {
        return parameters.getString("Username");
    }
    
    public String getPassword() {
        return parameters.getString("Password");
    }
    
    public WebClientProxy getWebClientProxy() {
        return clientProxy;
    }

}
