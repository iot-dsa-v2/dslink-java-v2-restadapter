package org.iot.dsa.dslink.restadapter;

import org.iot.dsa.node.DSLong;
import org.iot.dsa.node.DSMap;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;
import org.junit.Test;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

public class SubscriptionRuleTests {
    
    String body = "Prefix_[%STARTBLOCK%{%TIMESTAMP%:%VALUE%}%ENDBLOCK%]_Suffix";
    TimeZone utc = TimeZone.getTimeZone("UTC");
    
    @Test
    public void testBatchSendSingular() {
        TimeZone.setDefault(utc);
        Queue<SubUpdate> updates = new LinkedList<SubUpdate>();
        updates.add(new SubUpdate(DSDateTime.valueOf(2016, 10, 1, utc), DSLong.valueOf(42), DSStatus.ok));
        String expectedBody = "Prefix_[{2016-10-01T00:00:00.000Z:42}]_Suffix";
        testBatchSend(updates, expectedBody);
    }
    
    @Test
    public void testBatchSendPlural() {
        Queue<SubUpdate> updates = new LinkedList<SubUpdate>();
        updates.add(new SubUpdate(DSDateTime.valueOf(2016, 10, 1, utc), DSLong.valueOf(42), DSStatus.ok));
        updates.add(new SubUpdate(DSDateTime.valueOf(2016, 10, 2, utc), DSLong.valueOf(43), DSStatus.ok));
        updates.add(new SubUpdate(DSDateTime.valueOf(2016, 9, 2, utc), DSLong.valueOf(44), DSStatus.ok));
        String expectedBody = "Prefix_[{2016-10-01T00:00:00.000Z:42},{2016-10-02T00:00:00.000Z:43},{2016-09-02T00:00:00.000Z:44}]_Suffix";
        testBatchSend(updates, expectedBody);
    }
    
    private void testBatchSend(Queue<SubUpdate> updates, String expectedBody) {
//        MainNode.setRequester(requesterMock);
        
        WebClientProxy webClientProxy = mock(WebClientProxy.class);
        when(webClientProxy.invoke(anyString(), anyString(), any(), anyString()))
        .thenReturn(new Response.Builder()
                .request(new Request.Builder().url("https://api.buildingos.com/").build())
                .message("Success").protocol(Protocol.HTTP_2).code(200).build());
        
        AbstractRuleNode node = mock(AbstractRuleNode.class);
        when(node.getWebClientProxy()).thenReturn(webClientProxy);
        
        SubscriptionRule sr = new SubscriptionRule(node, "/example", "example.com", "POST", new DSMap(), body, 0, 0, 0);
        
        assertNull(sr.sendBatchUpdate(updates));
        
        verify(webClientProxy).invoke("POST", "example.com", new DSMap(), expectedBody);
    }

}
