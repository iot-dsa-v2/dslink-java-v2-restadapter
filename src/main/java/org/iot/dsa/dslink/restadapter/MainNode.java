package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.DSLinkConnection;
import org.iot.dsa.dslink.DSMainNode;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.node.event.DSIEvent;
import org.iot.dsa.node.event.DSISubscriber;
import org.iot.dsa.node.event.DSITopic;
import org.iot.dsa.util.DSException;

/**
 * The root and only node of this link.
 */
public class MainNode extends DSMainNode {


    private static final Object requesterLock = new Object();
    private static DSIRequester requester;

    public MainNode() {
    }

    public static DSIRequester getRequester() {
        synchronized (requesterLock) {
            while (requester == null) {
                try {
                    requesterLock.wait();
                } catch (InterruptedException e) {
                    DSException.throwRuntime(e);
                }
            }
            return requester;
        }
    }

    public static void setRequester(DSIRequester requester) {
        synchronized (requesterLock) {
            MainNode.requester = requester;
            requesterLock.notifyAll();
        }
    }

    /**
     * Defines the permanent children of this node type, their existence is guaranteed in all
     * instances.  This is only ever called once per, type per process.
     */
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.ACT_ADD_BASIC_CONN, makeAddBasicConnectionAction());
        declareDefault(Constants.ACT_ADD_OAUTH_CLIENT_CONN, makeAddOauthClientConnectionAction());
        declareDefault(Constants.ACT_ADD_OAUTH_PASSWORD_CONN, makeAddOauthPassConnectionAction());
        declareDefault("Docs", DSString.valueOf("https://github.com/iot-dsa-v2/dslink-java-v2-restadapter/blob/develop/docs/Usage_Guide.md")).setTransient(true).setReadOnly(true);
    }

    @Override
    protected void onStarted() {
        getLink().getConnection().subscribe(
                DSLinkConnection.CONNECTED, null, null,
                new DSISubscriber() {
                    @Override
                    public void onEvent(DSNode node, DSInfo child,
                                        DSIEvent event) {
                        MainNode.setRequester(getLink().getConnection().getRequester());
                    }

                    @Override
                    public void onUnsubscribed(DSITopic topic,
                                               DSNode node,
                                               DSInfo child) {
                    }
                });
    }

    private void addBasicConnection(DSMap parameters) {
        if (parameters.getString(Constants.USERNAME) == null) {
            parameters.put(Constants.CONNTYPE, DSString.valueOf(Util.AUTH_SCHEME.NO_AUTH));
        } else {
            parameters.put(Constants.CONNTYPE, DSString.valueOf(Util.AUTH_SCHEME.BASIC_USR_PASS));
        }
        String name = parameters.getString(Constants.NAME);
        put(name, new ConnectionNode(parameters));
    }

    private void addOAuthClientConnection(DSMap parameters) {
        parameters.put(Constants.CONNTYPE, DSString.valueOf(Util.AUTH_SCHEME.OAUTH2_CLIENT));
        String name = parameters.getString(Constants.NAME);
        put(name, new ConnectionNode(parameters));
    }

    private void addOAuthPasswordConnection(DSMap parameters) {
        parameters.put(Constants.CONNTYPE, DSString.valueOf(Util.AUTH_SCHEME.OAUTH2_USR_PASS));
        String name = parameters.getString(Constants.NAME);
        put(name, new ConnectionNode(parameters));
    }

    private DSAction makeAddBasicConnectionAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((MainNode) target.get()).addBasicConnection(invocation.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSValueType.STRING, null);
        act.addParameter(Constants.USERNAME, DSValueType.STRING, null);
        act.addParameter(Constants.PASSWORD, DSValueType.STRING, null).setEditor("password");
        return act;
    }

    private DSAction makeAddOauthClientConnectionAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((MainNode) target.get()).addOAuthClientConnection(invocation.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSValueType.STRING, null);
        act.addDefaultParameter(Constants.CLIENT_ID, DSString.EMPTY, null);
        act.addDefaultParameter(Constants.CLIENT_SECRET, DSString.EMPTY, null)
           .setEditor("password");
        act.addParameter(Constants.TOKEN_URL, DSString.EMPTY, null);
        return act;
    }

    private DSAction makeAddOauthPassConnectionAction() {
        DSAction act = new DSAction.Parameterless() {
            @Override
            public ActionResult invoke(DSInfo target, ActionInvocation invocation) {
                ((MainNode) target.get()).addOAuthPasswordConnection(invocation.getParameters());
                return null;
            }
        };
        act.addParameter(Constants.NAME, DSValueType.STRING, null);
        act.addParameter(Constants.USERNAME, DSValueType.STRING, null);
        act.addParameter(Constants.PASSWORD, DSValueType.STRING, null).setEditor("password");
        act.addParameter(Constants.CLIENT_ID, DSValueType.STRING, null);
        act.addParameter(Constants.CLIENT_SECRET, DSValueType.STRING, null).setEditor("password");
        act.addParameter(Constants.TOKEN_URL, DSValueType.STRING, null);
        return act;
    }
}
