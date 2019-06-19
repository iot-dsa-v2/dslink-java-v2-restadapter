package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.dslink.restadapter.Util.AUTH_SCHEME;
import org.iot.dsa.node.*;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;

public class ConnectionNode extends DSNode implements CredentialProvider {

    private DSMap parameters;
    private WebClientProxy clientProxy;

    public ConnectionNode() {

    }

    ConnectionNode(DSMap parameters) {
        this.parameters = parameters;
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.ACT_REMOVE, makeRemoveAction());
        declareDefault(Constants.ACT_ADD_RULE, makeAddRuleAction());
        declareDefault(Constants.ACT_ADD_RULE_TABLE, makeAddRuleTableAction());
    }

    @Override
    protected void onStarted() {
        super.onStarted();
        if (this.parameters == null) {
            DSIObject o = get(Constants.PARAMS);
            if (o instanceof DSMap) {
                this.parameters = (DSMap) o;
            }
        } else {
            put(Constants.PARAMS, parameters.copy()).setPrivate(true);
        }
    }

    @Override
    protected void onStable() {
        super.onStable();
        clientProxy = new WebClientProxy(this);
        put(Constants.ACT_EDIT, makeEditAction()).setTransient(true);
    }

    private DSAction makeAddRuleAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((ConnectionNode) target.get()).addRule(invocation.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSValueType.STRING, null);
        act.addParameter(Constants.SUB_PATH, DSValueType.STRING, null);
        act.addParameter(Constants.REST_URL, DSValueType.STRING, null);
        act.addDefaultParameter(Constants.REST_METHOD, DSString.valueOf("POST"), null);
        act.addDefaultParameter(Constants.URL_PARAMETERS, new DSMap(), null);
        act.addParameter(Constants.REQUEST_BODY, DSValueType.STRING, null);
        act.addParameter(Constants.MIN_REFRESH_RATE, DSValueType.NUMBER, "Optional, ensures at least this many seconds between updates");
        act.addParameter(Constants.MAX_REFRESH_RATE, DSValueType.NUMBER, "Optional, ensures an update gets sent every this many seconds");
        return act;
    }


    private void addRule(DSMap parameters) {
        String name = parameters.getString(Constants.NAME);
        put(name, new RuleNode(parameters));
    }

    private DSAction makeAddRuleTableAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((ConnectionNode) target.get()).addRuleTable(invocation.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSValueType.STRING, null);
        act.addDefaultParameter(Constants.RULE_TABLE, new DSList(), null);
        return act;
    }

    private void addRuleTable(DSMap parameters) {
        String name = parameters.getString(Constants.NAME);
        DSList table = parameters.getList(Constants.RULE_TABLE);
        put(name, new RuleTableNode(table));
    }

    private DSAction makeRemoveAction() {
        return new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((ConnectionNode) target.get()).delete();
                return null;
            }
        };
    }

    private void delete() {
        getParent().remove(getInfo());
    }

    private DSIObject makeEditAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((ConnectionNode) target.get()).edit(invocation.getParameters());
                return null;
            }
        };
        AUTH_SCHEME scheme = getAuthScheme();
        if (!Util.AUTH_SCHEME.OAUTH2_CLIENT.equals(scheme)) {
            act.addDefaultParameter(Constants.USERNAME, DSString.valueOf(getUsername()), null);
            act.addDefaultParameter(Constants.PASSWORD, DSString.valueOf(getPassword()), null).setEditor("password");
        }
        if (Util.AUTH_SCHEME.OAUTH2_CLIENT.equals(scheme) || Util.AUTH_SCHEME.OAUTH2_USR_PASS.equals(scheme)) {
            act.addDefaultParameter(Constants.CLIENT_ID, DSString.valueOf(getClientId()), null);
            act.addDefaultParameter(Constants.CLIENT_SECRET, DSString.valueOf(getClientSecret()), null).setEditor("password");
            act.addDefaultParameter(Constants.TOKEN_URL, DSString.valueOf(getTokenURL()), null);
        }
        return act;
    }

    private void edit(DSMap parameters) {
        for (Entry entry : parameters) {
            this.parameters.put(entry.getKey(), entry.getValue().copy());
        }
        put(Constants.PARAMS, parameters.copy());
        onStable();
    }

    public String getUsername() {
        return parameters.getString(Constants.USERNAME);
    }

    public String getPassword() {
        return parameters.getString(Constants.PASSWORD);
    }
    
    public String getClientId() {
        return parameters.getString(Constants.CLIENT_ID);
    }

    public String getClientSecret() {
        return parameters.getString(Constants.CLIENT_SECRET);
    }

    public String getTokenURL() {
        return parameters.getString(Constants.TOKEN_URL);
    }

    public AUTH_SCHEME getAuthScheme() {
        return AUTH_SCHEME.valueOf(parameters.getString(Constants.CONNTYPE));
    }

    public WebClientProxy getWebClientProxy() {
        return clientProxy;
    }

}
