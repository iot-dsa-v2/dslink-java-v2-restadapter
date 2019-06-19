package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.node.DSIObject;
import org.iot.dsa.node.DSInfo;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSNode;
import org.iot.dsa.node.action.ActionInvocation;
import org.iot.dsa.node.action.ActionResult;
import org.iot.dsa.node.action.DSAction;

public class TestRuleNode extends DSNode {
    
    private String subpath;
    private TestSubscriptionRule rule;
    
    public TestRuleNode() {
        
    }

    public TestRuleNode(String subpath) {
        this.subpath = subpath;
    }
    
    @Override
    protected void declareDefaults() {
        super.declareDefaults();
        declareDefault(Constants.ACT_REMOVE, makeRemoveAction());
        declareDefault("Refresh", makeRefreshAction());
    }
    
    private DSIObject makeRefreshAction() {
        return new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((TestRuleNode) info.getParent()).refresh();
                return null;
            }

            @Override
            public void prepareParameter(DSInfo target, DSMap parameter) {
                // TODO Auto-generated method stub
                
            }
        };
    }

    protected void refresh() {
        if (rule != null) {
            rule.close();
        }
        onStable();
    }

    @Override
    protected void onStarted() {
        super.onStarted();
        if (this.subpath == null) {
            DSIObject o = get("SubPath");
            if (o != null) {
                this.subpath = o.toString();
            }
        } else {
            put("SubPath", subpath).setPrivate(true);
        }
    }
    
    @Override
    protected void onStable() {
        super.onStable();
        rule = new TestSubscriptionRule(this, subpath);
//        put(Constants.ACT_EDIT, makeEditAction()).setTransient(true);
    }
    
    private DSAction makeRemoveAction() {
        return new DSAction() {
            @Override
            public ActionResult invoke(DSInfo info, ActionInvocation invocation) {
                ((TestRuleNode) info.getParent()).delete();
                return null;
            }

            @Override
            public void prepareParameter(DSInfo target, DSMap parameter) {
                // TODO Auto-generated method stub
                
            }
        };
    }

    private void delete() {
        if (rule != null) {
            rule.close();
        }
        getParent().remove(getInfo());
    }


}
