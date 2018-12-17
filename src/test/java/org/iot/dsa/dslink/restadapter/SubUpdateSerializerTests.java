package org.iot.dsa.dslink.restadapter;

import org.junit.Test;
import org.etsdb.ByteArrayBuilder;
import org.iot.dsa.dslink.restadapter.SubUpdate;
import org.iot.dsa.node.DSLong;
import org.iot.dsa.node.DSStatus;
import org.iot.dsa.time.DSDateTime;
import static org.junit.Assert.*;

public class SubUpdateSerializerTests {
    
    @Test
    public void testSerializer() {
        DSDateTime ts = DSDateTime.valueOf(2016, 10, 1);
        SubUpdate before = new SubUpdate(ts, DSLong.valueOf(42), DSStatus.ok);
        SubUpdateSerializer serializer = new SubUpdateSerializer();
        ByteArrayBuilder builder = new ByteArrayBuilder();
        serializer.toByteArray(builder, before, ts.timeInMillis());
        SubUpdate after = serializer.fromByteArray(builder, ts.timeInMillis());
        assertEquals(before.dateTime, after.dateTime);
        assertEquals(before.value, after.value);
        assertEquals(before.status, after.status);
        assertEquals(before.ts, after.ts);
    }
    

}
