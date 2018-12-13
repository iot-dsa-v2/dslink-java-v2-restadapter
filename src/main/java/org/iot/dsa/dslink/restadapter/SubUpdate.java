package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;

public class SubUpdate {
    
    public final String dateTime;
    public final String value;
    public final String status;
    public final long ts;
    
    public SubUpdate(String dateTime, String value, String status, long ts) {
        this.dateTime = dateTime;
        this.value = value;
        this.status = status;
        this.ts = ts;
    }
    
    public SubUpdate(DSDateTime dateTime, DSElement value, DSStatus status) {
        this(dateTime.toString(), value.toString(), status.toString(), dateTime.timeInMillis());
    }
}