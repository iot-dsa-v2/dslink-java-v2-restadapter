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
        declareDefault("Basic Connection", makeAddBasicConnectionAction());
        declareDefault("OAuth2 Client Flow", makeAddOauthClientConnectionAction());
        declareDefault("OAuth2 Passwrd Flow", makeAddOauthPassConnectionAction());
    }

    private DSAction makeAddBasicConnectionAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((MainNode) info.getParent()).addBasicConnection(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Username", DSValueType.STRING, null);
        act.addParameter("Password", DSValueType.STRING, null).setEditor("password");
        return act;
    }

    private DSAction makeAddOauthClientConnectionAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((MainNode) info.getParent()).addOAuthClientConnection(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addDefaultParameter("ClientID", DSString.valueOf(PrivateData.BRICK_CLI_CLIENT_ID), null);
        act.addDefaultParameter("ClientSecret", DSString.valueOf(PrivateData.BRICK_CLI_SECRET), null).setEditor("password");
        act.addDefaultParameter("TokenURL", DSString.valueOf(PrivateData.URL_TOKEN_PATH), null);
        return act;
    }

    private DSAction makeAddOauthPassConnectionAction() {
        DSAction act = new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((MainNode) info.getParent()).addOAuthPasswordConnection(invocation.getParameters());
                return null;
            }
        };
        act.addParameter("Name", DSValueType.STRING, null);
        act.addParameter("Username", DSValueType.STRING, null);
        act.addParameter("Password", DSValueType.STRING, null).setEditor("password");
        act.addParameter("ClientID", DSValueType.STRING, null);
        act.addParameter("ClientSecret", DSValueType.STRING, null).setEditor("password");
        act.addParameter("TokenURL", DSValueType.STRING, null);
        return act;
    }


    private void addOAuthClientConnection(DSMap parameters) {
        parameters.put("ConnType", DSString.valueOf(Util.AUTH_SCHEME.OAUTH2_CLIENT));
        String name = parameters.getString("Name");
        put(name, new ConnectionNode(parameters)).setTransient(true);
    }

    private void addOAuthPasswordConnection(DSMap parameters) {
        parameters.put("ConnType", DSString.valueOf(Util.AUTH_SCHEME.OAUTH2_USR_PASS));
        String name = parameters.getString("Name");
        put(name, new ConnectionNode(parameters)).setTransient(true);
    }

    private void addBasicConnection(DSMap parameters) {
        if (parameters.getString("Username").isEmpty()) {
            parameters.put("ConnType", DSString.valueOf(Util.AUTH_SCHEME.NO_AUTH));
        } else {
            parameters.put("ConnType", DSString.valueOf(Util.AUTH_SCHEME.NO_AUTH));
        }
        String name = parameters.getString("Name");
        put(name, new ConnectionNode(parameters)).setTransient(true);
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
