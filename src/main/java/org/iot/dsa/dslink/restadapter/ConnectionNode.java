package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.dslink.restadapter.Util.AUTH_SCHEME;
import org.iot.dsa.node.*;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.util.DSException;

public class ConnectionNode extends DSNode {

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
        switch (AUTH_SCHEME.valueOf(getAuthScheme())) {
            case NO_AUTH:
                clientProxy = WebClientProxy.buildNoAuthClient();
                break;
            case BASIC_USR_PASS:
                clientProxy = WebClientProxy.buildBasicUserPassClient(getUsername(), getPassword());
                break;
            case OAUTH2_CLIENT:
                clientProxy = WebClientProxy.buildClientFlowOAuth2Client(getClientId(), getClientSecret(), getTokenURL());
                break;
            case OAUTH2_USR_PASS:
                clientProxy = WebClientProxy.buildPasswordFlowOAuth2Client(getUsername(), getPassword(), getClientId(), getClientSecret(), getTokenURL());
                break;
            default:
                DSException.throwRuntime(new RuntimeException("Unsupported AuthScheme: " + getAuthScheme()));
        }
        put(Constants.ACT_EDIT, makeEditAction());
    }

    private DSAction makeAddRuleAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((ConnectionNode) info.getParent()).addRule(invocation.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSValueType.STRING, null);
        act.addParameter(Constants.SUB_PATH, DSValueType.STRING, null);
        act.addParameter(Constants.REST_URL, DSValueType.STRING, null);
        act.addDefaultParameter(Constants.REST_METHOD, DSString.valueOf("POST"), null);
        act.addDefaultParameter(Constants.URL_PARAMETERS, new DSMap(), null);
        act.addParameter(Constants.REQUEST_BODY, DSValueType.STRING, null);
        return act;
    }


    private void addRule(DSMap parameters) {
        String name = parameters.getString(Constants.NAME);
        put(name, new RuleNode(parameters)).setTransient(true);
    }

    private DSAction makeAddRuleTableAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((ConnectionNode) info.getParent()).addRuleTable(invocation.getParameters());
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
        put(name, new RuleTableNode(table)).setTransient(true);
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
        String scheme = getAuthScheme();
        if (!Util.AUTH_SCHEME.OAUTH2_CLIENT.equals(scheme)) {
            act.addDefaultParameter(Constants.USERNAME, DSString.valueOf(getUsername()), null);
            act.addDefaultParameter(Constants.PASSWORD, DSString.valueOf(getPassword()), null).setEditor("password");
        }
        if (Util.AUTH_SCHEME.OAUTH2_CLIENT.equals(scheme) || Util.AUTH_SCHEME.OAUTH2_USR_PASS.equals(scheme)) {
            act.addDefaultParameter(Constants.CLIENT_ID, DSString.valueOf(getClientId()), null);
            act.addParameter(Constants.CLIENT_SECRET, DSString.valueOf(getClientSecret()), null).setEditor("password");
        }
        return act;
    }

    private void edit(DSMap parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            Entry entry = parameters.getEntry(i);
            this.parameters.put(entry.getKey(), entry.getValue().copy());
        }
        put(Constants.PARAMS, parameters.copy());
        onStable();
    }

    private String getUsername() {
        return parameters.getString(Constants.USERNAME);
    }

    private String getPassword() {
        return parameters.getString(Constants.PASSWORD);
    }
    
    private String getClientId() {
        return parameters.getString(Constants.CLIENT_ID);
    }

    private String getClientSecret() {
        return parameters.getString(Constants.CLIENT_SECRET);
    }

    private String getTokenURL() {
        return parameters.getString(Constants.TOKEN_URL);
    }

    private String getAuthScheme() {
        return parameters.getString(Constants.CONNTYPE);
    }

    public WebClientProxy getWebClientProxy() {
        return clientProxy;
    }

}
