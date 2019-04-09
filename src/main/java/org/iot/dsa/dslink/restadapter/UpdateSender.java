package org.iot.dsa.dslink.restadapter;

import java.util.Queue;

public interface UpdateSender {
    
    public int getMaxBatchSize();
    
    public Queue<SubUpdate> sendBatchUpdate(Queue<SubUpdate> updates);

}
