package org.iot.dsa.dslink.restadapter;

import org.etsdb.util.PurgeSettings;
import org.iot.dsa.dslink.DSIRequester;
import org.iot.dsa.dslink.DSLinkConnection;
import org.iot.dsa.dslink.DSMainNode;
import org.iot.dsa.node.DSBool;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSLong;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSString;
import org.iot.dsa.node.DSValueType;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;
import org.iot.dsa.node.event.DSEventFilter;
import org.iot.dsa.util.DSException;

/**
 * The root node of this link.
 */
public class MainNode extends DSMainNode implements PurgeSettings {
    private static final Object requesterLock = new Object();
    private static DSIRequester requester;
    public static MainNode instance;

    private DSInfo maxBufferSize = getInfo(Constants.BUFFER_MAX_SIZE);
    private DSInfo purgeEnabled = getInfo(Constants.BUFFER_PURGE_ENABLED);

    public MainNode() {
    }

    public long getMaxSizeInBytes() {
        return maxBufferSize.getValue().toElement().toLong();
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

    public boolean isPurgeEnabled() {
        return purgeEnabled.getValue().toElement().toBoolean();
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
        declareDefault(Constants.BUFFER_PURGE_ENABLED, DSBool.FALSE,
                       "Whether old unsent records should automatically be purged from the buffer when the buffer gets too large");
        declareDefault(Constants.BUFFER_MAX_SIZE, DSLong.valueOf(1074000000),
                       "Maximum size of buffer in bytes; only applies if auto-purge is enabled");
        declareDefault("Docs", DSString.valueOf(
                "https://github.com/iot-dsa-v2/dslink-java-v2-restadapter/blob/develop/docs/Usage_Guide.md"))
                .setTransient(true).setReadOnly(true);
    }

    @Override
    protected void onStarted() {
        instance = this;
        getLink().getConnection().subscribe(new DSEventFilter(
                ((event, node, child, data) -> MainNode.setRequester(
                        getLink().getConnection().getRequester())),
                DSLinkConnection.CONNECTED_EVENT,
                null));
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
