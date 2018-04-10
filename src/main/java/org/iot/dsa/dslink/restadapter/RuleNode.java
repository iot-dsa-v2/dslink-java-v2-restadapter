package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;

public class RuleNode extends DSNode {
    
    private DSMap parameters;
    private SubscriptionRule rule;
    
    public RuleNode() {
        
    }
    
    public RuleNode(DSMap parameters) {
        this.parameters = parameters;
    }
    
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault("Remove", makeRemoveAction());
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
        rule = new SubscriptionRule((ConnectionNode) getParent(), getSubscribePath(), getRestUrl(), getMethod(), getURLParameters(), getBody());
        put("Edit", makeEditAction());
    }
    
    private DSAction makeRemoveAction() {
        return new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((RuleNode) info.getParent()).delete();
                return null;
            }
        };
    }

    private void delete() {
        if (rule != null) {
            rule.close();
        }
        getParent().remove(getInfo());
    }
    
    private DSIObject makeEditAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((RuleNode) info.getParent()).edit(invocation.getParameters());
                return null;
            }
        };
        act.addDefaultParameter("Subscribe Path", DSString.valueOf(getSubscribePath()), null);
        act.addDefaultParameter("REST URL", DSString.valueOf(getRestUrl()), null);
        act.addDefaultParameter("Method", DSString.valueOf(getMethod()), null);
        act.addDefaultParameter("URL Parameters", getURLParameters().copy(), null);
        act.addDefaultParameter("Body", DSString.valueOf(getBody()), null);
        return act;
    }

    protected void edit(DSMap parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            Entry entry = parameters.getEntry(i);
            this.parameters.put(entry.getKey(), entry.getValue().copy());
        }
        put("parameters", parameters.copy());
        if (rule != null) {
            rule.close();
        }
        onStable();
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
    
    public WebClientProxy getWebClientProxy() {
        return ((ConnectionNode) getParent()).getWebClientProxy();
    }

}
