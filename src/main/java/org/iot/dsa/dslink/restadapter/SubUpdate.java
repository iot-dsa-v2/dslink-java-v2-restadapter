package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.node.DSElement;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;

public class SubUpdate {
    final DSDateTime dateTime;
    final DSElement value;
    final DSStatus status;
    
    public SubUpdate(DSDateTime dateTime, DSElement value, DSStatus status) {
        this.dateTime = dateTime;
        this.value = value;
        this.status = status;
    }
}