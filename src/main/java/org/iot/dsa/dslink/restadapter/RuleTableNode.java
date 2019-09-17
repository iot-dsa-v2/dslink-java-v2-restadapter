package org.iot.dsa.dslink.restadapter;

import java.util.ArrayList;
import java.util.List;
import org.iot.dsa.dslink.ActionResults;
import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.node.action.DSIActionRequest;
import org.iot.dsa.time.DSDateTime;

public class RuleTableNode extends AbstractRuleNode {

    private DSInfo lastResponses = getInfo(Constants.LAST_RESPONSES_TABLE);
    private final List<SubscriptionRule> rules = new ArrayList<SubscriptionRule>();
    private DSList table;

    public RuleTableNode() {
    }

    public RuleTableNode(DSList table) {
        this.table = table;
    }

    @Override
    public void responseRecieved(ResponseWrapper resp, int rowNum) {
        DSList respTable = lastResponses.getElement().toList();
        DSMap respMap = respTable.getMap(rowNum);

        if (resp == null) {
            respMap.put(Constants.LAST_RESPONSE_CODE, -1);
            respMap.put(Constants.LAST_RESPONSE_DATA, "Failed to send update");
            respMap.put(Constants.LAST_RESPONSE_TS, DSDateTime.now().toString());
        } else {
            int status = resp.getCode();
            String data = resp.getData();

            respMap.put(Constants.LAST_RESPONSE_CODE, status);
            respMap.put(Constants.LAST_RESPONSE_DATA, data);
            respMap.put(Constants.LAST_RESPONSE_TS, resp.getTS().toString());
        }
        fire(VALUE_CHANGED_EVENT, lastResponses, null);
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.ACT_REMOVE, makeRemoveAction());
        declareDefault(Constants.LAST_RESPONSES_TABLE, new DSList()).setReadOnly(true);
    }

    protected void edit(DSMap parameters) {
        this.table = parameters.getList(Constants.RULE_TABLE).copy();
        put(Constants.RULE_TABLE, table.copy());
        closeRules();
        onStable();
    }

    @Override
    protected void onStable() {
        super.onStable();
        parseRules();
        put(Constants.ACT_EDIT, makeEditAction()).setTransient(true);
    }

    @Override
    protected void onStarted() {
        super.onStarted();
        if (this.table == null) {
            DSIObject o = get(Constants.RULE_TABLE);
            if (o instanceof DSList) {
                this.table = (DSList) o;
            }
        } else {
            put(Constants.RULE_TABLE, table.copy());
        }
    }

    private void closeRules() {
        for (SubscriptionRule rule : rules) {
            rule.close();
        }
        rules.clear();
    }

    private void delete() {
        closeRules();
        getParent().remove(getInfo());
    }

    private DSIObject makeEditAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResults invoke(DSIActionRequest req) {
                ((RuleTableNode) req.getTarget()).edit(req.getParameters());
                return null;
            }
        };
        act.addDefaultParameter(Constants.RULE_TABLE, table.copy(), null);
        return act;
    }

    private DSAction makeRemoveAction() {
        return new DSAction() {
            @Override
            public ActionResults invoke(DSIActionRequest req) {
                ((RuleTableNode) req.getTarget()).delete();
                return null;
            }
        };
    }

    private void parseRules() {
        DSList emptyResponseTable = new DSList();
        for (int i = 0; i < table.size(); i++) {
            emptyResponseTable.add(new DSMap());
            DSElement elem = table.get(i);
            String subPath, restUrl, method, body;
            DSMap urlParams;
            double minRefresh, maxRefresh;
            if (elem instanceof DSMap) {
                DSMap row = (DSMap) elem;
                subPath = row.getString(Constants.SUB_PATH);
                restUrl = row.getString(Constants.REST_URL);
                method = row.getString(Constants.REST_METHOD);
                urlParams = Util.dsElementToMap(row.get(Constants.URL_PARAMETERS));
                body = row.getString(Constants.REQUEST_BODY);
                minRefresh = Util.getDouble(row, Constants.MIN_REFRESH_RATE, 0.0);
                maxRefresh = Util.getDouble(row, Constants.MAX_REFRESH_RATE, 0.0);
                SubscriptionRule rule = new SubscriptionRule(this, subPath, restUrl, method,
                                                             urlParams, body, minRefresh,
                                                             maxRefresh, i);
                rules.add(rule);
            } else if (elem instanceof DSList) {
                DSList row = (DSList) elem;
                subPath = row.getString(1);
                restUrl = row.getString(2);
                method = row.getString(3);
                urlParams = Util.dsElementToMap(row.get(4));
                body = row.getString(5);
                minRefresh = Util.getDouble(row, 6, 0.0);
                maxRefresh = Util.getDouble(row, 7, 0.0);
                SubscriptionRule rule = new SubscriptionRule(this, subPath, restUrl, method,
                                                             urlParams, body, minRefresh,
                                                             maxRefresh, i);
                rules.add(rule);
            }
        }
        put(lastResponses, emptyResponseTable);
    }

}
