package org.iot.dsa.dslink.restadapter;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.node.event.DSValueTopic;

public class RuleTableNode extends AbstractRuleNode {
    
    private DSList table;
    private final List<SubscriptionRule> rules = new ArrayList<SubscriptionRule>();
    
    private DSInfo lastResponses = getInfo(Constants.LAST_RESPONSES_TABLE);
    
    public RuleTableNode() {
    }
    
    public RuleTableNode(DSList table) {
        this.table = table;
    }
    
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.ACT_REMOVE, makeRemoveAction());
        declareDefault(Constants.LAST_RESPONSES_TABLE, new DSList()).setReadOnly(true);
    }
    
    @Override
    protected void onStarted() {
        if (this.table == null) {
            DSIObject o = get(Constants.RULE_TABLE);
            if (o instanceof DSList) {
                this.table = (DSList) o;
            }
        } else {
            put(Constants.RULE_TABLE, table.copy()).setHidden(true);
        }
    }
    
    @Override
    protected void onStable() {
        parseRules();
        put(Constants.ACT_EDIT, makeEditAction());
    }
    
    private void parseRules() {
        DSList emptyResponseTable = new DSList();
        for (int i = 0; i < table.size(); i++) {
            emptyResponseTable.add(new DSMap());
            DSElement elem = table.get(i);
            String subPath, restUrl, method, body;
            DSMap urlParams;
            if (elem instanceof DSMap) {
                DSMap row = (DSMap) elem;
                subPath = row.getString(Constants.SUB_PATH);
                restUrl = row.getString(Constants.REST_URL);
                method = row.getString(Constants.REST_METHOD);
                urlParams = Util.dsElementToMap(row.get(Constants.URL_PARAMETERS));
                body = row.getString(Constants.REQUEST_BODY);
                SubscriptionRule rule = new SubscriptionRule(this, subPath, restUrl, method, urlParams, body, i);
                rules.add(rule);
            } else if (elem instanceof DSList) {
                DSList row = (DSList) elem;
                subPath = row.getString(1);
                restUrl = row.getString(2);
                method = row.getString(3);
                urlParams = Util.dsElementToMap(row.get(4));
                body = row.getString(5);
                SubscriptionRule rule = new SubscriptionRule(this, subPath, restUrl, method, urlParams, body, i);
                rules.add(rule);
            }
        }
        put(lastResponses, emptyResponseTable);
    }
    
    private void closeRules() {
        for (SubscriptionRule rule: rules) {
            rule.close();
        }
        rules.clear();
    }
    
    private DSAction makeRemoveAction() {
        return new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((RuleTableNode) info.getParent()).delete();
                return null;
            }
        };
    }

    private void delete() {
        closeRules();
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
        act.addDefaultParameter(Constants.RULE_TABLE, table.copy(), null);
        return act;
    }

    protected void edit(DSMap parameters) {
        this.table = parameters.getList(Constants.RULE_TABLE).copy();
        put(Constants.RULE_TABLE, table.copy());
        closeRules();
        onStable();
    }

    @Override
    public void responseRecieved(Response resp, int rowNum) {
        int status = resp.getStatus();
        String data = resp.readEntity(String.class);
        
        DSList respTable = lastResponses.getElement().toList();
        DSMap respMap = respTable.getMap(rowNum);
        respMap.put(Constants.LAST_RESPONSE_CODE, status);
        respMap.put(Constants.LAST_RESPONSE_DATA, data);
        fire(VALUE_TOPIC, DSValueTopic.Event.CHILD_CHANGED, lastResponses);
    }

}
