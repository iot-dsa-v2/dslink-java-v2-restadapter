package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.dslink.ActionResults;
import org.iot.dsa.dslink.restadapter.Util.AUTH_SCHEME;
import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSList;
import org.iot.dsa.node.DSLong;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSMap.Entry;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.node.action.DSIActionRequest;

public class ConnectionNode extends DSNode implements CredentialProvider {

    private WebClientProxy clientProxy;
    private DSMap parameters;

    public ConnectionNode() {

    }

    ConnectionNode(DSMap parameters) {
        this.parameters = parameters;
    }

    public AUTH_SCHEME getAuthScheme() {
        return AUTH_SCHEME.valueOf(parameters.getString(Constants.CONNTYPE));
    }

    public String getClientId() {
        return parameters.getString(Constants.CLIENT_ID);
    }

    public String getClientSecret() {
        return parameters.getString(Constants.CLIENT_SECRET);
    }

    public String getPassword() {
        return parameters.getString(Constants.PASSWORD);
    }

    public String getTokenURL() {
        return parameters.getString(Constants.TOKEN_URL);
    }

    public String getUsername() {
        return parameters.getString(Constants.USERNAME);
    }

    public String getToken() {
        return parameters.getString(Constants.TOKEN);
    }

    public WebClientProxy getWebClientProxy() {
        return clientProxy;
    }

    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.ACT_REMOVE, makeRemoveAction());
        declareDefault(Constants.ACT_ADD_RULE, makeAddRuleAction());
        declareDefault(Constants.ACT_ADD_RULE_TABLE, makeAddRuleTableAction());
    }

    @Override
    protected void onStable() {
        super.onStable();
        clientProxy = new WebClientProxy(this);
        put(Constants.ACT_EDIT, makeEditAction()).setTransient(true);
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

    private void addRule(DSMap parameters) {
        String name = parameters.getString(Constants.NAME);
        put(name, new RuleNode(parameters));
    }

    private void addRuleTable(DSMap parameters) {
        String name = parameters.getString(Constants.NAME);
        DSList table = parameters.getList(Constants.RULE_TABLE);
        put(name, new RuleTableNode(table));
    }

    private void delete() {
        getParent().remove(getInfo());
    }

    private void edit(DSMap parameters) {
        for (Entry entry : parameters) {
            this.parameters.put(entry.getKey(), entry.getValue().copy());
        }
        put(Constants.PARAMS, parameters.copy());
        onStable();
    }

    private DSAction makeAddRuleAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResults invoke(DSIActionRequest req) {
                ((ConnectionNode) req.getTarget()).addRule(req.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSString.NULL, null);
        act.addParameter(Constants.SUB_PATH, DSString.NULL, null);
        act.addParameter(Constants.REST_URL, DSString.NULL, null);
        act.addDefaultParameter(Constants.REST_METHOD, DSString.valueOf("POST"), null);
        act.addDefaultParameter(Constants.URL_PARAMETERS, new DSMap(), null);
        act.addParameter(Constants.REQUEST_BODY, DSString.NULL, null);
        act.addParameter(Constants.MIN_REFRESH_RATE, DSLong.NULL,
                         "Optional, ensures at least this many seconds between updates");
        act.addParameter(Constants.MAX_REFRESH_RATE, DSLong.NULL,
                         "Optional, ensures an update gets sent every this many seconds");
        return act;
    }

    private DSAction makeAddRuleTableAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResults invoke(DSIActionRequest req) {
                ((ConnectionNode) req.getTarget()).addRuleTable(req.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSString.NULL, null);
        act.addDefaultParameter(Constants.RULE_TABLE, new DSList(), null);
        return act;
    }

    private DSIObject makeEditAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResults invoke(DSIActionRequest req) {
                ((ConnectionNode) req.getTarget()).edit(req.getParameters());
                return null;
            }
        };

        AUTH_SCHEME scheme = getAuthScheme();
        if (AUTH_SCHEME.BASIC_USR_PASS.equals(scheme)) {
            act.addDefaultParameter(Constants.USERNAME, DSString.valueOf(getUsername()), null);
            act.addDefaultParameter(Constants.PASSWORD, DSString.valueOf(getPassword()), null)
               .setEditor("password");
        }

        if (Util.AUTH_SCHEME.OAUTH2_CLIENT.equals(scheme) || Util.AUTH_SCHEME.OAUTH2_USR_PASS
                .equals(scheme)) {
            act.addDefaultParameter(Constants.CLIENT_ID, DSString.valueOf(getClientId()), null);
            act.addDefaultParameter(Constants.CLIENT_SECRET, DSString.valueOf(getClientSecret()),
                                    null).setEditor("password");
            act.addDefaultParameter(Constants.TOKEN_URL, DSString.valueOf(getTokenURL()), null);
        }

        if (AUTH_SCHEME.BEARER.equals(scheme)) {
            act.addDefaultParameter(Constants.TOKEN_URL, DSString.valueOf(getToken()), null);
        }

        return act;
    }

    private DSAction makeRemoveAction() {
        return new DSAction() {
            @Override
            public ActionResults invoke(DSIActionRequest req) {
                ((ConnectionNode) req.getTarget()).delete();
                return null;
            }
        };
    }

}
