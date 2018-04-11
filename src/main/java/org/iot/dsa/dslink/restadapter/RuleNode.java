package org.iot.dsa.dslink.restadapter;

import javax.ws.rs.core.Response;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSInt;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;

public class RuleNode extends AbstractRuleNode {
    
    private DSMap parameters;
    private SubscriptionRule rule;
    
    private DSInfo lastRespCode = getInfo(Constants.LAST_RESPONSE_CODE);
    private DSInfo lastRespData = getInfo(Constants.LAST_RESPONSE_DATA);
    
    public RuleNode() {
        
    }
    
    public RuleNode(DSMap parameters) {
        this.parameters = parameters;
    }
    
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.ACT_REMOVE, makeRemoveAction());
        declareDefault(Constants.LAST_RESPONSE_CODE, DSInt.NULL).setReadOnly(true);
        declareDefault(Constants.LAST_RESPONSE_DATA, DSString.EMPTY).setReadOnly(true);
    }
    
    @Override
    protected void onStarted() {
        if (this.parameters == null) {
            DSIObject o = get(Constants.PARAMS);
            if (o instanceof DSMap) {
                this.parameters = (DSMap) o;
            }
        } else {
            put(Constants.PARAMS, parameters.copy()).setHidden(true);
        }
    }
    
    @Override
    protected void onStable() {
        rule = new SubscriptionRule(this, getSubscribePath(), getRestUrl(), getMethod(), getURLParameters(), getBody(), 0);
        put(Constants.ACT_EDIT, makeEditAction());
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
        act.addDefaultParameter(Constants.SUB_PATH, DSString.valueOf(getSubscribePath()), null);
        act.addDefaultParameter(Constants.REST_URL, DSString.valueOf(getRestUrl()), null);
        act.addDefaultParameter(Constants.REST_METHOD, DSString.valueOf(getMethod()), null);
        act.addDefaultParameter(Constants.URL_PARAMETERS, getURLParameters().copy(), null);
        act.addDefaultParameter(Constants.REQUEST_BODY, DSString.valueOf(getBody()), null);
        return act;
    }

    protected void edit(DSMap parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            Entry entry = parameters.getEntry(i);
            this.parameters.put(entry.getKey(), entry.getValue().copy());
        }
        put(Constants.PARAMS, parameters.copy());
        if (rule != null) {
            rule.close();
        }
        onStable();
    }
    
    public String getSubscribePath() {
        return parameters.getString(Constants.SUB_PATH);
    }
    
    public String getRestUrl() {
        return parameters.getString(Constants.REST_URL);
    }
    
    public String getMethod() {
        return parameters.getString(Constants.REST_METHOD);
    }
    
    public DSMap getURLParameters() {
        return parameters.getMap(Constants.URL_PARAMETERS);
    }
    
    public String getBody() {
        return parameters.getString(Constants.REQUEST_BODY);
    }

    @Override
    public void responseRecieved(Response resp, int rowNum) {
        int status = resp.getStatus();
        String data = resp.readEntity(String.class);
        
        put(lastRespCode, DSInt.valueOf(status));
        put(lastRespData, DSString.valueOf(data));
    }

}
